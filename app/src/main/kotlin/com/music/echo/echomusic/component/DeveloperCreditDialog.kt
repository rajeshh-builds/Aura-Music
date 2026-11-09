package echo.music.iad1tya.echomusic.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import echo.music.iad1tya.R
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

/**
 * Developer Credit Dialog displayed on application launch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperCreditDialog(
  onDismiss: () -> Unit
) {
  val cardShape =
    AbsoluteSmoothCornerShape(
      cornerRadiusTL = 28.dp,
      cornerRadiusTR = 28.dp,
      cornerRadiusBL = 28.dp,
      cornerRadiusBR = 28.dp,
      smoothnessAsPercentTL = 60,
      smoothnessAsPercentTR = 60,
      smoothnessAsPercentBL = 60,
      smoothnessAsPercentBR = 60,
    )
  val blockShape = AbsoluteSmoothCornerShape(20.dp, 60)
  val actionShape = AbsoluteSmoothCornerShape(16.dp, 60)

  BasicAlertDialog(onDismissRequest = onDismiss) {
    Surface(
      modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
      shape = cardShape,
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      tonalElevation = 8.dp,
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Surface(
          shape = blockShape,
          color = MaterialTheme.colorScheme.surfaceContainer,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Surface(
              shape = AbsoluteSmoothCornerShape(12.dp, 60),
              color = MaterialTheme.colorScheme.primaryContainer,
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  painter = painterResource(R.drawable.star),
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text = "Official Developer Build",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
              }
            }

            Text(
              text = "Aura Music",
              style = MaterialTheme.typography.headlineMedium,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.onSurface,
              textAlign = TextAlign.Center
            )

            Text(
              text = "Developed & Maintained by Rajesh",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.primary,
              textAlign = TextAlign.Center
            )
          }
        }

        Surface(
          shape = blockShape,
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = "• Fully Customized & Bloat-Free",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "• High-Fidelity Audio Streaming & Caching",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "• Enhanced Security & Zero Annoying Updates",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "• Crafted with ❤️ for seamless music listening",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }

        Button(
          onClick = onDismiss,
          shape = actionShape,
          modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
          Text(
            text = "Enjoy Music",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}
