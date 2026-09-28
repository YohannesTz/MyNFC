package com.github.yohannestz.mynfc.ui.scan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.yohannestz.mynfc.nfc.NfcStatus
import com.github.yohannestz.mynfc.ui.common.openNfcSettings
import com.github.yohannestz.mynfc.ui.common.rememberNfcStatus
import com.github.yohannestz.mynfc.ui.components.CircleIconButton
import com.github.yohannestz.mynfc.ui.components.PrimaryPillButton
import com.github.yohannestz.mynfc.ui.components.ScanPulse
import com.github.yohannestz.mynfc.ui.components.SecondaryPillButton
import com.github.yohannestz.mynfc.ui.theme.Blue
import com.github.yohannestz.mynfc.ui.theme.Sky
import com.github.yohannestz.mynfc.ui.theme.Violet

/** Waiting room while reader mode is active. Tag reads are routed globally to the detail screen. */
@Composable
fun ScanScreen(onClose: () -> Unit) {
    val status = rememberNfcStatus()
    val context = LocalContext.current
    val background = MaterialTheme.colorScheme.background

    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.verticalGradient(listOf(Sky.copy(alpha = 0.22f), Violet.copy(alpha = 0.10f), background), endY = size.height * 0.7f))
            drawCircle(Blue.copy(alpha = 0.10f), radius = size.width * 0.7f, center = Offset(size.width * 0.5f, -size.width * 0.15f))
        }
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                CircleIconButton(Icons.Rounded.Close, "Close", onClose)
                Text(
                    "Read NFC",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            Spacer(Modifier.weight(0.6f))
            ScanPulse(color = Blue, size = 280.dp)
            Spacer(Modifier.height(32.dp))

            if (status == NfcStatus.READY) {
                Text("Ready to Scan", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Move your phone slowly over the tag.\nTry the top, middle and back of your device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(
                    if (status == NfcStatus.DISABLED) "NFC is off" else "NFC not supported",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (status == NfcStatus.DISABLED) "Turn on NFC in settings to start scanning." else "This device can't read NFC tags.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (status == NfcStatus.DISABLED) {
                    Spacer(Modifier.height(20.dp))
                    PrimaryPillButton("Open NFC settings", { context.openNfcSettings() }, icon = Icons.Rounded.Tune)
                }
            }

            Spacer(Modifier.weight(0.4f))
            Row(Modifier.fillMaxWidth()) {
                Tip(Icons.Rounded.PhoneAndroid, "Remove thick\nphone cases", Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                Tip(Icons.Rounded.WifiTethering, "Hold still for\n1–2 seconds", Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            SecondaryPillButton("Cancel", onClose, Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun Tip(icon: ImageVector, text: String, modifier: Modifier) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, modifier = modifier) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Blue, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
