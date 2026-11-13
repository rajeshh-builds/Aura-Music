package echo.music.iad1tya.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import echo.music.iad1tya.R
import echo.music.iad1tya.utils.makeTimeString
import kotlinx.coroutines.delay
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RingtoneTrimmerDialog(
  isVisible: Boolean,
  songId: String?,
  songTitle: String?,
  duration: Long,
  onDismiss: () -> Unit,
  onResolveStreamUrl: suspend (String) -> String?,
  onConfirm: (Long, Long) -> Unit,
) {
  if (!isVisible || songId == null) return

  val context = LocalContext.current

  val exoPlayer = remember {
    ExoPlayer.Builder(context).build().apply { repeatMode = Player.REPEAT_MODE_OFF }
  }

  var isPlaying by remember { mutableStateOf(false) }
  var isLoading by remember { mutableStateOf(false) }

  val ringtoneWindowSec = 30f
  val safeDurationSec = if (duration > 0) duration.toFloat() / 1000f else 180f
  val maxStartSec = (safeDurationSec - ringtoneWindowSec).coerceAtLeast(0f)

  // Smart highlights for best drops/chorus (35% and 65% of song)
  val hook1Start = (safeDurationSec * 0.35f).coerceIn(0f, maxStartSec)
  val hook2Start = (safeDurationSec * 0.65f).coerceIn(0f, maxStartSec)

  var startSec by remember(songId) {
    mutableFloatStateOf(if (safeDurationSec >= 60f) hook1Start else 0f)
  }

  var currentPlaybackSec by remember { mutableFloatStateOf(startSec) }

  LaunchedEffect(songId, isVisible) {
    if (isVisible) {
      isLoading = true
      val resolvedUrl = onResolveStreamUrl(songId)
      val uri = resolvedUrl?.let { android.net.Uri.parse(it) }

      if (uri != null) {
        val mediaItem = MediaItem.Builder().setUri(uri).build()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
      }
      isLoading = false
    }
  }

  DisposableEffect(Unit) { onDispose { exoPlayer.release() } }

  LaunchedEffect(isPlaying, startSec) {
    if (isPlaying) {
      val endMs = ((startSec + ringtoneWindowSec) * 1000).toLong()
      while (isPlaying) {
        val pos = exoPlayer.currentPosition
        currentPlaybackSec = (pos / 1000f).coerceIn(startSec, startSec + ringtoneWindowSec)
        if (pos >= endMs) {
          exoPlayer.pause()
          exoPlayer.seekTo((startSec * 1000).toLong())
          currentPlaybackSec = startSec
          isPlaying = false
        }
        delay(80)
      }
    }
  }

  val barCount = 70
  val waveformHeights = remember(songId, barCount) {
    val seed = songId.hashCode().toLong()
    val random = java.util.Random(seed)
    List(barCount) { i ->
      val progress = i.toFloat() / barCount
      val chorusWeight = when {
        progress in 0.30f..0.48f -> 0.35f
        progress in 0.60f..0.78f -> 0.45f
        else -> 0.1f
      }
      val wave = (sin(i * 0.35) * 0.25).toFloat().coerceAtLeast(0f)
      val rnd = random.nextFloat() * 0.4f
      (0.2f + wave + rnd + chorusWeight).coerceIn(0.15f, 1.0f)
    }
  }

  AlertDialog(
    onDismissRequest = {
      exoPlayer.stop()
      onDismiss()
    },
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFF10E77E), Color(0xFF1DB954)))),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Rounded.GraphicEq,
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier.size(20.dp)
          )
        }
        Column {
          Text(
            "Instagram-Style Ringtone",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          Text(
            "Slide 30s Window • Best Drops Highlighted",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF10E77E)
          )
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = songTitle ?: "Unknown Track",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          color = Color.White.copy(alpha = 0.9f),
          maxLines = 1
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Jump Chips: Best Parts (Chorus / Hook)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center
        ) {
          if (safeDurationSec >= 45f) {
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = if (kotlin.math.abs(startSec - hook1Start) < 2f) Color(0xFF1DB954) else Color(0xFF1A261E),
              modifier = Modifier
                .padding(horizontal = 4.dp)
                .clickable {
                  startSec = hook1Start
                  exoPlayer.seekTo((startSec * 1000).toLong())
                  currentPlaybackSec = startSec
                  if (isPlaying) exoPlayer.play()
                }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Rounded.AutoAwesome,
                  contentDescription = null,
                  modifier = Modifier.size(14.dp),
                  tint = if (kotlin.math.abs(startSec - hook1Start) < 2f) Color.Black else Color(0xFFFFD54F)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  "Hook 1 (${makeTimeString((hook1Start * 1000).toLong())})",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = if (kotlin.math.abs(startSec - hook1Start) < 2f) Color.Black else Color.White
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(16.dp),
              color = if (kotlin.math.abs(startSec - hook2Start) < 2f) Color(0xFFFF8A00) else Color(0xFF261D15),
              modifier = Modifier
                .padding(horizontal = 4.dp)
                .clickable {
                  startSec = hook2Start
                  exoPlayer.seekTo((startSec * 1000).toLong())
                  currentPlaybackSec = startSec
                  if (isPlaying) exoPlayer.play()
                }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Rounded.AutoAwesome,
                  contentDescription = null,
                  modifier = Modifier.size(14.dp),
                  tint = if (kotlin.math.abs(startSec - hook2Start) < 2f) Color.Black else Color(0xFFFF8A00)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  "Climax (${makeTimeString((hook2Start * 1000).toLong())})",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = if (kotlin.math.abs(startSec - hook2Start) < 2f) Color.Black else Color.White
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        val endSec = (startSec + ringtoneWindowSec).coerceAtMost(safeDurationSec)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Start: ${makeTimeString((startSec * 1000).toLong())}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF10E77E)
          )
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1DB954).copy(alpha = 0.2f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1DB954))
          ) {
            Text(
              text = "Exact 30 Sec Window",
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF10E77E)
            )
          }
          Text(
            text = "End: ${makeTimeString((endSec * 1000).toLong())}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF10E77E)
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // INSTAGRAM-STYLE SOUNDWAVE SCRUBBER CANVAS
        var canvasWidthPx by remember { mutableFloatStateOf(1f) }

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0C140F))
            .border(1.dp, Color(0xFF1B2E22), RoundedCornerShape(16.dp))
            .onGloballyPositioned { coordinates ->
              canvasWidthPx = coordinates.size.width.toFloat()
            }
            .pointerInput(safeDurationSec, maxStartSec) {
              detectHorizontalDragGestures(
                onHorizontalDrag = { _, dragAmount ->
                  if (canvasWidthPx > 0f && maxStartSec > 0f) {
                    val deltaSec = (dragAmount / canvasWidthPx) * safeDurationSec
                    val newStart = (startSec + deltaSec).coerceIn(0f, maxStartSec)
                    startSec = newStart
                    exoPlayer.seekTo((newStart * 1000).toLong())
                    currentPlaybackSec = newStart
                  }
                }
              )
            }
        ) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            val totalW = size.width
            val totalH = size.height

            val windowStartFraction = if (safeDurationSec > 0f) startSec / safeDurationSec else 0f
            val windowEndFraction = if (safeDurationSec > 0f) (startSec + ringtoneWindowSec) / safeDurationSec else 1f

            val windowStartPx = windowStartFraction * totalW
            val windowEndPx = windowEndFraction * totalW
            val windowWidthPx = (windowEndPx - windowStartPx).coerceAtLeast(8f)

            // Draw Recommended Chorus Zones on the track
            if (safeDurationSec >= 45f) {
              val h1StartPx = (hook1Start / safeDurationSec) * totalW
              val h1EndPx = ((hook1Start + ringtoneWindowSec) / safeDurationSec) * totalW
              drawRoundRect(
                color = Color(0xFFFFD54F).copy(alpha = 0.12f),
                topLeft = Offset(h1StartPx, 0f),
                size = Size(h1EndPx - h1StartPx, totalH),
                cornerRadius = CornerRadius(8f, 8f)
              )

              val h2StartPx = (hook2Start / safeDurationSec) * totalW
              val h2EndPx = ((hook2Start + ringtoneWindowSec) / safeDurationSec) * totalW
              drawRoundRect(
                color = Color(0xFFFF8A00).copy(alpha = 0.12f),
                topLeft = Offset(h2StartPx, 0f),
                size = Size(h2EndPx - h2StartPx, totalH),
                cornerRadius = CornerRadius(8f, 8f)
              )
            }

            // Draw Instagram-style glowing 30s selection frame
            drawRoundRect(
              brush = Brush.verticalGradient(
                listOf(Color(0xFF1DB954).copy(alpha = 0.25f), Color(0xFF10E77E).copy(alpha = 0.10f))
              ),
              topLeft = Offset(windowStartPx, 0f),
              size = Size(windowWidthPx, totalH),
              cornerRadius = CornerRadius(12f, 12f)
            )

            // Highlight border for the 30s selected frame
            drawRoundRect(
              color = Color(0xFF10E77E),
              topLeft = Offset(windowStartPx, 0f),
              size = Size(windowWidthPx, totalH),
              cornerRadius = CornerRadius(12f, 12f),
              style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
            )

            // Draw soundwave bars across the track
            val step = totalW / barCount
            val barW = (step * 0.6f).coerceAtLeast(2f)

            waveformHeights.forEachIndexed { index, normalizedHeight ->
              val barX = index * step + step / 2f
              val barH = normalizedHeight * (totalH * 0.72f)
              val topY = (totalH - barH) / 2f

              val isInsideSelection = barX in windowStartPx..windowEndPx
              val progressAlongTrack = index.toFloat() / barCount

              val barColor = when {
                isInsideSelection -> Color(0xFF10E77E)
                safeDurationSec >= 45f && progressAlongTrack in (hook1Start / safeDurationSec)..((hook1Start + ringtoneWindowSec) / safeDurationSec) -> Color(0xFFFFD54F)
                safeDurationSec >= 45f && progressAlongTrack in (hook2Start / safeDurationSec)..((hook2Start + ringtoneWindowSec) / safeDurationSec) -> Color(0xFFFF8A00)
                else -> Color(0xFF2E4535)
              }

              drawRoundRect(
                color = barColor,
                topLeft = Offset(barX - barW / 2f, topY),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(3f, 3f)
              )
            }

            // Current playback needle (scrub head)
            if (isPlaying && safeDurationSec > 0f) {
              val needleX = (currentPlaybackSec / safeDurationSec) * totalW
              drawLine(
                color = Color.White,
                start = Offset(needleX, 4f),
                end = Offset(needleX, totalH - 4f),
                strokeWidth = 3.dp.toPx()
              )
              drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = Offset(needleX, 8f)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "👈 Drag window horizontally to choose any 30s drop 👉",
          style = MaterialTheme.typography.labelSmall,
          color = Color.White.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Play / Pause Preview Button
        Box(
          modifier = Modifier.align(Alignment.CenterHorizontally),
          contentAlignment = Alignment.Center
        ) {
          if (isLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(52.dp),
              color = Color(0xFF10E77E)
            )
          } else {
            Surface(
              shape = CircleShape,
              color = Color(0xFF1DB954),
              modifier = Modifier
                .size(52.dp)
                .clickable {
                  if (isPlaying) {
                    exoPlayer.pause()
                    isPlaying = false
                  } else {
                    exoPlayer.seekTo((startSec * 1000).toLong())
                    exoPlayer.play()
                    isPlaying = true
                  }
                }
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                  contentDescription = if (isPlaying) "Pause Preview" else "Play Preview",
                  tint = Color.Black,
                  modifier = Modifier.size(32.dp)
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          exoPlayer.stop()
          val start = (startSec * 1000).toLong()
          val end = minOf(((startSec + ringtoneWindowSec) * 1000).toLong(), (safeDurationSec * 1000).toLong())
          onConfirm(start, end)
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = Color(0xFF1DB954),
          contentColor = Color.Black
        ),
        shape = RoundedCornerShape(20.dp)
      ) {
        Text("Set 30s Ringtone", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(
        onClick = {
          exoPlayer.stop()
          onDismiss()
        }
      ) {
        Text(stringResource(R.string.cancel), color = Color.White.copy(alpha = 0.7f))
      }
    },
    containerColor = Color(0xFF0E1711),
    shape = RoundedCornerShape(24.dp)
  )
}
