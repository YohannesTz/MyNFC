package com.github.yohannestz.mynfc.ui.rfid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.yohannestz.mynfc.data.model.RfidDump
import com.github.yohannestz.mynfc.ui.common.LocalAppContainer
import com.github.yohannestz.mynfc.ui.common.LocalSnackbar
import com.github.yohannestz.mynfc.ui.common.fileStamp
import com.github.yohannestz.mynfc.ui.common.relativeTime
import com.github.yohannestz.mynfc.ui.common.rememberJsonExporter
import com.github.yohannestz.mynfc.ui.common.rememberJsonImporter
import com.github.yohannestz.mynfc.ui.components.CircleIconButton
import com.github.yohannestz.mynfc.ui.components.EmptyState
import com.github.yohannestz.mynfc.ui.components.GradientActionCard
import com.github.yohannestz.mynfc.ui.components.IconBadge
import com.github.yohannestz.mynfc.ui.components.PrimaryPillButton
import com.github.yohannestz.mynfc.ui.components.ScreenHeader
import com.github.yohannestz.mynfc.ui.components.SectionLabel
import com.github.yohannestz.mynfc.ui.theme.Gradients
import com.github.yohannestz.mynfc.ui.theme.Indigo
import kotlinx.coroutines.launch

@Composable
fun RfidScreen(onScan: () -> Unit, onOpen: (String) -> Unit) {
    val repository = LocalAppContainer.current.repository
    val dumps by repository.savedDumps.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    var menu by remember { mutableStateOf(false) }

    val exporter = rememberJsonExporter { ok ->
        scope.launch { snackbar.showSnackbar(if (ok) "Dumps exported" else "Export failed") }
    }
    val importer = rememberJsonImporter { text ->
        scope.launch {
            val msg = runCatching { repository.importRfidJson(text) }
                .fold({ "Imported $it dump(s)" }, { "Not a valid MyNFC RFID export" })
            snackbar.showSnackbar(msg)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        ScreenHeader(
            title = "RFID",
            subtitle = "Raw tag memory",
            action = {
                Box {
                    CircleIconButton(Icons.Rounded.MoreVert, "More", { menu = true })
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Import dumps") },
                            leadingIcon = { Icon(Icons.Rounded.FileUpload, null) },
                            onClick = { menu = false; importer() },
                        )
                    }
                }
            },
        )

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                GradientActionCard(
                    title = "Scan RFID tag",
                    description = "Dump a chip's raw memory — MIFARE Classic, Ultralight / NTAG and ISO 15693 — then view, edit, save or clone it.",
                    icon = Icons.Rounded.Sensors,
                    brush = Gradients.rfid,
                    onClick = onScan,
                )
            }

            if (dumps.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 40.dp)) {
                        EmptyState(
                            Icons.Rounded.Memory,
                            "No dumps yet",
                            "Scan a 13.56 MHz tag to read its memory block by block. Saved dumps and JSON exports appear here.",
                            Modifier.align(Alignment.TopCenter),
                        ) {
                            PrimaryPillButton("Scan a tag", onScan, icon = Icons.Rounded.Sensors)
                        }
                    }
                }
            } else {
                item { SectionLabel("Saved dumps") }
                items(dumps, key = { it.id }) { dump -> DumpCard(dump) { onOpen(dump.id) } }
                item {
                    Surface(
                        onClick = { exporter.export("mynfc-rfid-${fileStamp()}.json") { repository.exportRfidJson() } },
                        shape = MaterialTheme.shapes.large,
                        color = Indigo.copy(alpha = 0.10f),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Memory, null, tint = Indigo)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Export all dumps", style = MaterialTheme.typography.titleSmall, color = Indigo)
                                Text("Every saved dump in one JSON file", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DumpCard(dump: RfidDump, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.CreditCard, Indigo, size = 48.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(dump.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                    Spacer(Modifier.width(8.dp))
                    Text(relativeTime(dump.scannedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
                Text(
                    dump.uid,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Text(
                    "${dump.readableBlocks} ${dump.family.unit}s · ${dump.totalBytes} bytes" + if (dump.partial) " · partial" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
