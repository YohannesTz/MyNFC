package com.github.yohannestz.mynfc.ui.rfid

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
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Tune
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
import com.github.yohannestz.mynfc.ui.theme.Indigo
import com.github.yohannestz.mynfc.ui.theme.Purple

@Composable
fun RfidScanScreen(onClose: () -> Unit) {
    val status = rememberNfcStatus()
    val context = LocalContext.current
    val background = MaterialTheme.colorScheme.background

    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.verticalGradient(listOf(Indigo.copy(alpha = 0.22f), Purple.copy(alpha = 0.10f), background), endY = size.height * 0.7f))
            drawCircle(Indigo.copy(alpha = 0.10f), radius = size.width * 0.7f, center = Offset(size.width * 0.5f, -size.width * 0.15f))
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
                Text("Scan RFID", style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.Center))
            }

            Spacer(Modifier.weight(0.6f))
            ScanPulse(color = Indigo, size = 280.dp, icon = Icons.Rounded.Memory)
            Spacer(Modifier.height(32.dp))

            if (status == NfcStatus.READY) {
                Text("Reading raw memory", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Hold a 13.56 MHz tag to the back of your phone.\nClassic sectors with default keys are dumped automatically.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(if (status == NfcStatus.DISABLED) "NFC is off" else "NFC not supported", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (status == NfcStatus.DISABLED) "Turn on NFC to read tags." else "This device can't read tags.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (status == NfcStatus.DISABLED) {
                    Spacer(Modifier.height(20.dp))
                    PrimaryPillButton("Open NFC settings", { context.openNfcSettings() }, icon = Icons.Rounded.Tune, containerColor = Indigo)
                }
            }

            Spacer(Modifier.weight(0.4f))
            Row(Modifier.fillMaxWidth()) {
                Tip(Icons.Rounded.CreditCard, "Access cards\n& fobs", Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                Tip(Icons.Rounded.Memory, "125 kHz tags\nneed a reader", Modifier.weight(1f))
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
            Icon(icon, null, tint = Indigo, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
