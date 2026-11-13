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
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
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

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
        delay(60)
      }
    }
  }

  val barCount = 64
  val waveformHeights = remember(songId, barCount) {
    val seed = songId.hashCode().toLong()
    val random = java.util.Random(seed)
    List(barCount) { i ->
      val progress = i.toFloat() / barCount
      val chorusWeight = when {
        progress in 0.30f..0.48f -> 0.35f
        progress in 0.60f..0.78f -> 0.45f
        else -> 0.10f
      }
      val wave = (sin(i * 0.35) * 0.25).toFloat().coerceAtLeast(0f)
      val rnd = random.nextFloat() * 0.35f
      (0.25f + wave + rnd + chorusWeight).coerceIn(0.20f, 1.0f)
    }
  }

  ModalBottomSheet(
    onDismissRequest = {
      exoPlayer.stop()
      onDismiss()
    },
    sheetState = sheetState,
    containerColor = Color(0xFF080D0A),
    scrimColor = Color.Black.copy(alpha = 0.75f),
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    dragHandle = {
      Box(
        modifier = Modifier
          .padding(top = 12.dp, bottom = 8.dp)
          .size(width = 40.dp, height = 4.dp)
          .clip(CircleShape)
          .background(Color.White.copy(alpha = 0.25f))
      )
    }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 22.dp)
        .padding(bottom = 28.dp)
    ) {
      // Header Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(
                Brush.linearGradient(
                  listOf(Color(0xFF10E77E), Color(0xFF1DB954))
                )
              ),
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
              text = "Ringtone Studio",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
            Text(
              text = "Instagram Reel-Style 30s Window",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFF10E77E)
            )
          }
        }

        IconButton(
          onClick = {
            exoPlayer.stop()
            onDismiss()
          },
          modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close",
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(18.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Track Card with Track Name & Duration
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF121B15),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1B2E22)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = songTitle ?: "Unknown Track",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "Total Length: ${makeTimeString((safeDurationSec * 1000).toLong())}",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White.copy(alpha = 0.6f)
            )
          }

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF10E77E).copy(alpha = 0.15f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10E77E).copy(alpha = 0.4f))
          ) {
            Text(
              text = "30s Bit",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF10E77E),
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // AI Smart Highlights / Jump Chips
      if (safeDurationSec >= 45f) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Hook 1 Chip
          val isHook1Active = kotlin.math.abs(startSec - hook1Start) < 2f
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isHook1Active) Color(0xFFFFD54F) else Color(0xFF1E281F),
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              if (isHook1Active) Color(0xFFFFD54F) else Color(0xFFFFD54F).copy(alpha = 0.3f)
            ),
            modifier = Modifier
              .weight(1f)
              .clickable {
                startSec = hook1Start
                exoPlayer.seekTo((startSec * 1000).toLong())
                currentPlaybackSec = startSec
                if (isPlaying) exoPlayer.play()
              }
          ) {
            Row(
              modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isHook1Active) Color.Black else Color(0xFFFFD54F)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Chorus (${makeTimeString((hook1Start * 1000).toLong())})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isHook1Active) Color.Black else Color.White
              )
            }
          }

          // Climax / Drop Chip
          val isHook2Active = kotlin.math.abs(startSec - hook2Start) < 2f
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isHook2Active) Color(0xFFFF8A00) else Color(0xFF281C16),
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              if (isHook2Active) Color(0xFFFF8A00) else Color(0xFFFF8A00).copy(alpha = 0.3f)
            ),
            modifier = Modifier
              .weight(1f)
              .clickable {
                startSec = hook2Start
                exoPlayer.seekTo((startSec * 1000).toLong())
                currentPlaybackSec = startSec
                if (isPlaying) exoPlayer.play()
              }
          ) {
            Row(
              modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isHook2Active) Color.Black else Color(0xFFFF8A00)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Drop (${makeTimeString((hook2Start * 1000).toLong())})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isHook2Active) Color.Black else Color.White
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
      }

      // Time Range Indicators
      val endSec = (startSec + ringtoneWindowSec).coerceAtMost(safeDurationSec)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "START TIME",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 10.sp
          )
          Text(
            text = makeTimeString((startSec * 1000).toLong()),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF10E77E)
          )
        }

        Surface(
          shape = RoundedCornerShape(20.dp),
          color = Color(0xFF1DB954).copy(alpha = 0.15f),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10E77E).copy(alpha = 0.3f))
        ) {
          Text(
            text = "30s Playing Window",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF10E77E),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
          )
        }

        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "END TIME",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 10.sp
          )
          Text(
            text = makeTimeString((endSec * 1000).toLong()),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF10E77E)
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // INSTAGRAM REEL-STYLE SOUNDWAVE SCRUBBER CANVAS
      var canvasWidthPx by remember { mutableFloatStateOf(1f) }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(90.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(Color(0xFF0D1611))
          .border(1.dp, Color(0xFF1C2D22), RoundedCornerShape(16.dp))
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
          val windowWidthPx = (windowEndPx - windowStartPx).coerceAtLeast(10f)

          // Background highlight for Chorus 1 zone (Yellow)
          if (safeDurationSec >= 45f) {
            val h1StartPx = (hook1Start / safeDurationSec) * totalW
            val h1EndPx = ((hook1Start + ringtoneWindowSec) / safeDurationSec) * totalW
            drawRoundRect(
              color = Color(0xFFFFD54F).copy(alpha = 0.12f),
              topLeft = Offset(h1StartPx, 0f),
              size = Size(h1EndPx - h1StartPx, totalH),
              cornerRadius = CornerRadius(6f, 6f)
            )

            // Background highlight for Climax zone (Orange)
            val h2StartPx = (hook2Start / safeDurationSec) * totalW
            val h2EndPx = ((hook2Start + ringtoneWindowSec) / safeDurationSec) * totalW
            drawRoundRect(
              color = Color(0xFFFF8A00).copy(alpha = 0.12f),
              topLeft = Offset(h2StartPx, 0f),
              size = Size(h2EndPx - h2StartPx, totalH),
              cornerRadius = CornerRadius(6f, 6f)
            )
          }

          // Active 30s Window Gradient
          drawRoundRect(
            brush = Brush.verticalGradient(
              listOf(Color(0xFF1DB954).copy(alpha = 0.28f), Color(0xFF10E77E).copy(alpha = 0.12f))
            ),
            topLeft = Offset(windowStartPx, 0f),
            size = Size(windowWidthPx, totalH),
            cornerRadius = CornerRadius(12f, 12f)
          )

          // Glowing Emerald Frame Stroke
          drawRoundRect(
            color = Color(0xFF10E77E),
            topLeft = Offset(windowStartPx, 0f),
            size = Size(windowWidthPx, totalH),
            cornerRadius = CornerRadius(12f, 12f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
          )

          // Left & Right Drag Handle Grips (Instagram style tactile notches)
          val handleBarWidth = 4.dp.toPx()
          val handleBarHeight = 24.dp.toPx()
          drawRoundRect(
            color = Color.White,
            topLeft = Offset(windowStartPx + 4.dp.toPx(), (totalH - handleBarHeight) / 2f),
            size = Size(handleBarWidth, handleBarHeight),
            cornerRadius = CornerRadius(2f, 2f)
          )
          drawRoundRect(
            color = Color.White,
            topLeft = Offset(windowEndPx - 4.dp.toPx() - handleBarWidth, (totalH - handleBarHeight) / 2f),
            size = Size(handleBarWidth, handleBarHeight),
            cornerRadius = CornerRadius(2f, 2f)
          )

          // Soundwave Bars
          val step = totalW / barCount
          val barW = (step * 0.55f).coerceAtLeast(2.5f)

          waveformHeights.forEachIndexed { index, normalizedHeight ->
            val barX = index * step + step / 2f
            val barH = normalizedHeight * (totalH * 0.68f)
            val topY = (totalH - barH) / 2f

            val isInsideSelection = barX in windowStartPx..windowEndPx
            val progressAlongTrack = index.toFloat() / barCount

            val barColor = when {
              isInsideSelection -> Color(0xFF10E77E)
              safeDurationSec >= 45f && progressAlongTrack in (hook1Start / safeDurationSec)..((hook1Start + ringtoneWindowSec) / safeDurationSec) -> Color(0xFFFFD54F)
              safeDurationSec >= 45f && progressAlongTrack in (hook2Start / safeDurationSec)..((hook2Start + ringtoneWindowSec) / safeDurationSec) -> Color(0xFFFF8A00)
              else -> Color(0xFF263C2E)
            }

            drawRoundRect(
              color = barColor,
              topLeft = Offset(barX - barW / 2f, topY),
              size = Size(barW, barH),
              cornerRadius = CornerRadius(2f, 2f)
            )
          }

          // Live Playback Needle
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

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "👈 Drag window anywhere across soundwave to select any 30s portion 👉",
        style = MaterialTheme.typography.labelSmall,
        color = Color.White.copy(alpha = 0.5f),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(20.dp))

      // Bottom Action Bar: [Play Preview Button (Circle)] + [Set as Ringtone (Large Pill)]
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Preview Play/Pause Button
        Surface(
          shape = CircleShape,
          color = Color(0xFF1DB954),
          modifier = Modifier
            .size(54.dp)
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
            if (isLoading) {
              CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.Black,
                strokeWidth = 2.5.dp
              )
            } else {
              Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause Preview" else "Play Preview",
                tint = Color.Black,
                modifier = Modifier.size(30.dp)
              )
            }
          }
        }

        // Set as Ringtone Button (Expanded Pill)
        Button(
          onClick = {
            exoPlayer.stop()
            val start = (startSec * 1000).toLong()
            val end = minOf(((startSec + ringtoneWindowSec) * 1000).toLong(), (safeDurationSec * 1000).toLong())
            onConfirm(start, end)
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF10E77E),
            contentColor = Color.Black
          ),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .weight(1f)
            .height(54.dp)
        ) {
          Text(
            text = "Set as Ringtone (30s)",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
          )
        }
      }
    }
  }
}
