package com.github.yohannestz.mynfc.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.yohannestz.mynfc.BuildConfig
import com.github.yohannestz.mynfc.nfc.NfcOperation
import com.github.yohannestz.mynfc.nfc.NfcStatus
import com.github.yohannestz.mynfc.ui.common.LocalAppContainer
import com.github.yohannestz.mynfc.ui.common.LocalSnackbar
import com.github.yohannestz.mynfc.ui.common.fileStamp
import com.github.yohannestz.mynfc.ui.common.openNfcSettings
import com.github.yohannestz.mynfc.ui.common.rememberJsonExporter
import com.github.yohannestz.mynfc.ui.common.rememberJsonImporter
import com.github.yohannestz.mynfc.ui.common.rememberNfcStatus
import com.github.yohannestz.mynfc.ui.components.GradientActionCard
import com.github.yohannestz.mynfc.ui.components.IconBadge
import com.github.yohannestz.mynfc.ui.components.InfoCard
import com.github.yohannestz.mynfc.ui.components.InfoRow
import com.github.yohannestz.mynfc.ui.components.ScreenHeader
import com.github.yohannestz.mynfc.ui.components.SectionLabel
import com.github.yohannestz.mynfc.ui.theme.Blue
import com.github.yohannestz.mynfc.ui.theme.Emerald
import com.github.yohannestz.mynfc.ui.theme.Gradients
import com.github.yohannestz.mynfc.ui.theme.Pink
import com.github.yohannestz.mynfc.ui.theme.Purple
import com.github.yohannestz.mynfc.ui.theme.Red
import kotlinx.coroutines.launch

@Composable
fun ToolsScreen() {
    val container = LocalAppContainer.current
    val controller = container.nfcController
    val repository = container.repository
    val tags by repository.savedTags.collectAsStateWithLifecycle()
    val records by repository.records.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val status = rememberNfcStatus()
    var confirmLock by remember { mutableStateOf(false) }
    var confirmErase by remember { mutableStateOf(false) }

    val exporter = rememberJsonExporter { ok ->
        scope.launch { snackbar.showSnackbar(if (ok) "Library exported" else "Export failed") }
    }
    val importer = rememberJsonImporter { text ->
        scope.launch {
            val message = runCatching { repository.importJson(text) }
                .fold({ (t, r) -> "Imported $t tag(s) and $r record(s)" }, { "Not a valid MyNFC export file" })
            snackbar.showSnackbar(message)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        ScreenHeader(title = "Advanced")
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            GradientActionCard(
                title = "Copy a tag",
                description = "Read the data from one tag and write an identical copy onto another in two taps.",
                icon = Icons.Rounded.ContentCopy,
                brush = Gradients.tools,
                onClick = { controller.start(NfcOperation.Clone) },
            )

            SectionLabel("Tag operations")
            InfoCard {
                ToolRow(Icons.Rounded.DeleteSweep, Pink, "Erase tag", "Remove all data, keep the tag writable") { confirmErase = true }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ToolRow(Icons.Rounded.RestartAlt, Purple, "Format tag", "Prepare a blank tag for NDEF") { controller.start(NfcOperation.Format) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ToolRow(Icons.Rounded.Lock, Red, "Lock tag", "Make it permanently read-only") { confirmLock = true }
            }

            SectionLabel("Data")
            InfoCard {
                ToolRow(Icons.Rounded.FileDownload, Blue, "Export to JSON", "${tags.size} tag(s) · ${records.size} record(s)") {
                    exporter.export("mynfc-export-${fileStamp()}.json") { repository.exportJson() }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ToolRow(Icons.Rounded.FileUpload, Emerald, "Import from JSON", "Merge a MyNFC export into your library", onClick = importer)
            }

            SectionLabel("Device")
            InfoCard {
                InfoRow(
                    "NFC",
                    when (status) {
                        NfcStatus.READY -> "Enabled"
                        NfcStatus.DISABLED -> "Disabled"
                        NfcStatus.UNSUPPORTED -> "Not supported"
                    },
                    valueColor = if (status == NfcStatus.READY) Emerald else Red,
                    trailing = if (status == NfcStatus.DISABLED) {
                        { TextButton(onClick = { context.openNfcSettings() }) { Text("Settings") } }
                    } else null,
                )
                InfoRow("App version", BuildConfig.VERSION_NAME, showDivider = false)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmErase) {
        ConfirmDialog(
            title = "Erase tag?",
            text = "All data on the next tag you tap will be replaced with an empty record.",
            confirm = "Erase",
            onDismiss = { confirmErase = false },
            onConfirm = { confirmErase = false; controller.start(NfcOperation.Erase) },
        )
    }
    if (confirmLock) {
        ConfirmDialog(
            title = "Lock tag permanently?",
            text = "This cannot be undone. The tag will become read-only forever and can never be rewritten or erased.",
            confirm = "Lock forever",
            onDismiss = { confirmLock = false },
            onConfirm = { confirmLock = false; controller.start(NfcOperation.Lock) },
        )
    }
}

@Composable
private fun ToolRow(icon: ImageVector, color: Color, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(icon, color)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ConfirmDialog(title: String, text: String, confirm: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.WarningAmber, null, tint = Red) },
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, color = Red) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
