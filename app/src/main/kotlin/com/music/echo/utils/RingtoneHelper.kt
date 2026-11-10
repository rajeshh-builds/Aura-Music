package echo.music.iad1tya.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.getSystemService
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import echo.music.iad1tya.constants.AudioQuality
import echo.music.iad1tya.utils.YTPlayerUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object RingtoneHelper {

  private val okHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(30, TimeUnit.SECONDS)
      .readTimeout(60, TimeUnit.SECONDS)
      .followRedirects(true)
      .followSslRedirects(true)
      .build()
  }

  suspend fun getStreamUrl(context: Context, songId: String): String? =
    withContext(Dispatchers.IO) {
      try {
        val connectivityManager = context.getSystemService<ConnectivityManager>()!!
        val audioQuality = AudioQuality.OPUS

        val result =
          YTPlayerUtils.playerResponseForPlayback(
            videoId = songId,
            audioQuality = audioQuality,
            connectivityManager = connectivityManager,
          )
        result.getOrNull()?.streamUrl
      } catch (e: Exception) {
        e.printStackTrace()
        null
      }
    }

  suspend fun downloadAndTrimAsRingtone(
    context: Context,
    songId: String,
    title: String,
    artist: String,
    startMs: Long,
    endMs: Long,
    onProgress: (Float, String) -> Unit,
    onComplete: (Boolean, String, Uri?) -> Unit
  ) =
    withContext(Dispatchers.IO) {
      val tempSourceFile = File(context.cacheDir, "temp_ringtone_source_${songId}_${System.currentTimeMillis()}.raw")
      val tempTrimmedMp3 = File(context.cacheDir, "temp_ringtone_trimmed_${songId}_${System.currentTimeMillis()}.mp3")

      try {
        onProgress(0.05f, "Getting audio stream...")

        val streamUrl = getStreamUrl(context, songId)
        if (streamUrl == null) {
          withContext(Dispatchers.Main) { onComplete(false, "Failed to get audio stream", null) }
          return@withContext
        }

        onProgress(0.15f, "Fetching audio stream...")

        val request = Request.Builder()
          .url(streamUrl)
          .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0")
          .header("Range", "bytes=0-")
          .header("Referer", "https://music.youtube.com/")
          .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful || response.body == null) {
          withContext(Dispatchers.Main) {
            onComplete(false, "Download failed: HTTP ${response.code}", null)
          }
          return@withContext
        }

        val body = response.body!!
        val contentLength = body.contentLength()
        val inputStream = body.byteStream()
        val outputStream = FileOutputStream(tempSourceFile)

        val buffer = ByteArray(64 * 1024)
        var totalBytesRead = 0L
        var bytesRead: Int

        inputStream.use { input ->
          outputStream.use { output ->
            while (input.read(buffer).also { bytesRead = it } != -1) {
              output.write(buffer, 0, bytesRead)
              totalBytesRead += bytesRead
              if (contentLength > 0) {
                val p = 0.15f + (totalBytesRead.toFloat() / contentLength) * 0.45f
                onProgress(p.coerceIn(0.15f, 0.60f), "Downloading audio...")
              }
            }
          }
        }

        onProgress(0.65f, "Trimming & encoding to MP3...")

        val trimSuccess = trimAndEncodeToMp3(tempSourceFile, tempTrimmedMp3, startMs, endMs, title, artist)
        if (!trimSuccess || !tempTrimmedMp3.exists() || tempTrimmedMp3.length() <= 0L) {
          withContext(Dispatchers.Main) {
            onComplete(false, "Failed to encode ringtone MP3", null)
          }
          return@withContext
        }

        onProgress(0.85f, "Saving to phone ringtones...")

        val ringtoneUri = saveToRingtonesFolder(context, tempTrimmedMp3, title, artist)

        if (ringtoneUri != null) {
          onProgress(1.0f, "Ringtone ready!")
          withContext(Dispatchers.Main) {
            onComplete(true, "Ringtone created successfully!", ringtoneUri)
          }
        } else {
          withContext(Dispatchers.Main) {
            onComplete(false, "Failed to save ringtone to device", null)
          }
        }

      } catch (e: Exception) {
        e.printStackTrace()
        withContext(Dispatchers.Main) {
          onComplete(false, "Error: ${e.localizedMessage ?: e.message}", null)
        }
      } finally {
        try {
          if (tempSourceFile.exists()) tempSourceFile.delete()
          if (tempTrimmedMp3.exists()) tempTrimmedMp3.delete()
        } catch (_: Exception) {}
      }
    }

  private fun trimAndEncodeToMp3(
    inputFile: File,
    outputFile: File,
    startMs: Long,
    endMs: Long,
    title: String,
    artist: String
  ): Boolean {
    return try {
      val startSec = startMs / 1000.0
      val durationSec = ((endMs - startMs).coerceAtLeast(1000L)) / 1000.0

      val escapedIn = inputFile.absolutePath.replace("'", "'\\''")
      val escapedOut = outputFile.absolutePath.replace("'", "'\\''")
      val safeTitle = title.replace("'", "'\\''")
      val safeArtist = artist.replace("'", "'\\''")

      // Fast seek with -ss before -i and re-encode to high quality standard MP3
      val command = "-y -ss $startSec -t $durationSec -i '$escapedIn' -vn -c:a libmp3lame -b:a 192k -metadata title='$safeTitle' -metadata artist='$safeArtist' '$escapedOut'"

      val session = FFmpegKit.execute(command)
      val returnCode = session.returnCode
      returnCode != null && ReturnCode.isSuccess(returnCode) && outputFile.exists() && outputFile.length() > 0L
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }

  private fun saveToRingtonesFolder(
    context: Context,
    mp3File: File,
    title: String,
    artist: String
  ): Uri? {
    return try {
      val fileName = "${sanitizeFilename(title)}_ringtone_${System.currentTimeMillis()}.mp3"

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
          put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
          put(MediaStore.Audio.Media.TITLE, title)
          put(MediaStore.Audio.Media.ARTIST, artist)
          put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
          put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_RINGTONES)
          put(MediaStore.Audio.Media.IS_RINGTONE, 1)
          put(MediaStore.Audio.Media.IS_NOTIFICATION, 0)
          put(MediaStore.Audio.Media.IS_ALARM, 0)
          put(MediaStore.Audio.Media.IS_MUSIC, 0)
        }

        val uri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
          context.contentResolver.openOutputStream(uri)?.use { output ->
            mp3File.inputStream().use { input ->
              input.copyTo(output)
            }
          }
          uri
        } else {
          null
        }
      } else {
        @Suppress("DEPRECATION")
        val ringtonesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES)
        if (!ringtonesDir.exists()) ringtonesDir.mkdirs()
        val destFile = File(ringtonesDir, fileName)
        mp3File.copyTo(destFile, overwrite = true)

        val values = ContentValues().apply {
          put(MediaStore.Audio.Media.DATA, destFile.absolutePath)
          put(MediaStore.Audio.Media.TITLE, title)
          put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
          put(MediaStore.Audio.Media.IS_RINGTONE, 1)
        }
        context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
      }
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  private fun sanitizeFilename(input: String): String =
    input.replace(Regex("[\\\\/:*?\"<>|]"), "_").replace(Regex("\\s+"), " ").trim().ifBlank {
      "ringtone_${System.currentTimeMillis()}"
    }

  fun openRingtoneSettings(context: Context, ringtoneUri: Uri? = null) {
    try {
      val intent =
        Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
          putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
          putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Ringtone")
          if (ringtoneUri != null) {
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, ringtoneUri)
          }
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      context.startActivity(intent)
    } catch (e: Exception) {
      val intent =
        Intent(Settings.ACTION_SOUND_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
      context.startActivity(intent)
    }
  }

  fun hasSettingsPermission(context: Context): Boolean = true

  fun requestSettingsPermission(context: Context) {}
}
