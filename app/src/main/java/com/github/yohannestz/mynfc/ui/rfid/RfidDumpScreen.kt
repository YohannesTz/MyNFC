package com.github.yohannestz.mynfc.ui.rfid

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.yohannestz.mynfc.data.model.MemoryBlock
import com.github.yohannestz.mynfc.data.model.RfidDump
import com.github.yohannestz.mynfc.nfc.RawOperation
import com.github.yohannestz.mynfc.ui.common.LocalAppContainer
import com.github.yohannestz.mynfc.ui.common.LocalSnackbar
import com.github.yohannestz.mynfc.ui.common.copyToClipboard
import com.github.yohannestz.mynfc.ui.common.fileStamp
import com.github.yohannestz.mynfc.ui.common.formatDate
import com.github.yohannestz.mynfc.ui.common.rememberJsonExporter
import com.github.yohannestz.mynfc.ui.common.shareText
import com.github.yohannestz.mynfc.ui.components.CircleIconButton
import com.github.yohannestz.mynfc.ui.components.EmptyState
import com.github.yohannestz.mynfc.ui.components.InfoCard
import com.github.yohannestz.mynfc.ui.components.InfoRow
import com.github.yohannestz.mynfc.ui.components.PrimaryPillButton
import com.github.yohannestz.mynfc.ui.components.ScreenHeader
import com.github.yohannestz.mynfc.ui.components.SecondaryPillButton
import com.github.yohannestz.mynfc.ui.components.SectionLabel
import com.github.yohannestz.mynfc.ui.theme.Amber
import com.github.yohannestz.mynfc.ui.theme.Emerald
import com.github.yohannestz.mynfc.ui.theme.Gradients
import com.github.yohannestz.mynfc.ui.theme.Indigo
import com.github.yohannestz.mynfc.ui.theme.Purple
import com.github.yohannestz.mynfc.ui.theme.Red
import kotlinx.coroutines.launch

private fun roleColor(role: String): Color = when (role) {
    "manufacturer" -> Amber
    "sector-trailer" -> Red
    "config", "lock" -> Purple
    else -> Indigo
}

private fun roleLabel(role: String): String = when (role) {
    "manufacturer" -> "UID / manufacturer"
    "sector-trailer" -> "Sector trailer (keys & access bits)"
    "config" -> "Configuration"
    "lock" -> "Lock bits"
    else -> "Data"
}

@Composable
fun RfidDumpScreen(id: String, onClose: () -> Unit) {
    val container = LocalAppContainer.current
    val repository = container.repository
    val saved by repository.savedDumps.collectAsStateWithLifecycle()
    val recent by repository.recentDumps.collectAsStateWithLifecycle()
    val savedDump = saved.firstOrNull { it.id == id }
    val dump = savedDump ?: recent[id]
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()

    var editing by remember { mutableStateOf<MemoryBlock?>(null) }
    var showSave by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var confirmRestore by remember { mutableStateOf(false) }

    val exporter = rememberJsonExporter { ok ->
        scope.launch { snackbar.showSnackbar(if (ok) "Dump exported" else "Export failed") }
    }
    fun toast(m: String) = scope.launch { snackbar.showSnackbar(m) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        ScreenHeader(
            title = "Tag memory",
            subtitle = dump?.let { formatDate(it.scannedAt) },
            navigation = { CircleIconButton(Icons.Rounded.Close, "Close", onClose) },
            action = {
                if (savedDump != null) CircleIconButton(Icons.Rounded.DeleteOutline, "Delete", { showDelete = true }, tint = Red)
                else if (dump != null) CircleIconButton(Icons.Rounded.Share, "Share", { context.shareText("RFID dump ${dump.uid}", report(dump)) })
            },
        )

        if (dump == null) {
            EmptyState(Icons.Rounded.Memory, "Dump not found", "This scan is no longer available.", Modifier.weight(1f))
            return@Column
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            HeroCard(dump, saved = savedDump != null)

            if (dump.info.isNotEmpty()) {
                SectionLabel("Chip")
                InfoCard {
                    dump.info.forEachIndexed { i, e -> InfoRow(e.label, e.value, showDivider = i != dump.info.lastIndex) }
                }
            }

            if (dump.partial) {
                Spacer(Modifier.height(12.dp))
                Surface(shape = MaterialTheme.shapes.large, color = Amber.copy(alpha = 0.12f)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lock, null, tint = Amber)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "${dump.sectorsRead} of ${dump.sectorsTotal} sectors used the factory key. Sectors with custom keys are shown locked.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            SectionLabel("Memory · ${dump.blocks.size} ${dump.family.unit}s")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                dump.blocks.forEach { block ->
                    BlockRow(block, unit = dump.family.unit) { if (block.writable && block.readable) editing = block }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (savedDump == null) {
                    PrimaryPillButton("Save dump", { showSave = true }, Modifier.fillMaxWidth(), icon = Icons.Rounded.BookmarkAdd, containerColor = Indigo)
                } else {
                    PrimaryPillButton("Rename", { showSave = true }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Edit, containerColor = Emerald)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryPillButton("Restore to tag", { confirmRestore = true }, Modifier.weight(1f), icon = Icons.Rounded.RestartAlt)
                    SecondaryPillButton(
                        "Export",
                        { exporter.export("rfid-${dump.uid.replace(":", "")}-${fileStamp()}.json") { repository.exportRfidJson(listOf(dump)) } },
                        Modifier.weight(1f),
                        icon = Icons.Rounded.DataObject,
                        outlined = true,
                    )
                }
            }
        }
    }

    editing?.let { block ->
        HexEditorDialog(
            block = block,
            unit = dump!!.family.unit,
            onDismiss = { editing = null },
            onWrite = { bytesHex ->
                editing = null
                container.nfcController.startRaw(
                    RawOperation.WriteUnit(dump.family, block.index, bytesHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray(), title = "Write ${dump.family.unit} ${block.index}"),
                )
            },
        )
    }

    if (showSave && dump != null) {
        NameDialog(
            initial = dump.name ?: dump.tagType,
            title = if (savedDump == null) "Save dump" else "Rename dump",
            onDismiss = { showSave = false },
            onConfirm = { name ->
                repository.saveDump(dump.copy(name = name.ifBlank { null }))
                showSave = false
                toast(if (savedDump == null) "Dump saved" else "Renamed")
            },
        )
    }
    if (showDelete && savedDump != null) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete dump?") },
            text = { Text("\"${savedDump.displayName}\" will be removed.") },
            confirmButton = { TextButton(onClick = { showDelete = false; repository.deleteDump(savedDump.id); onClose() }) { Text("Delete", color = Red) } },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } },
        )
    }
    if (confirmRestore && dump != null) {
        val writable = dump.blocks.count { it.writable && it.readable }
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            icon = { Icon(Icons.Rounded.RestartAlt, null, tint = Red) },
            title = { Text("Restore onto another tag?") },
            text = { Text("This writes $writable writable ${dump.family.unit}(s) from this dump onto a blank ${dump.family.label} tag. Block 0 (UID) and locked pages are skipped.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = false
                    container.nfcController.startRaw(RawOperation.Restore(dump.blocks, dump.family))
                }) { Text("Restore", color = Red) }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun HeroCard(dump: RfidDump, saved: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Gradients.rfid),
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(alpha = 0.08f), radius = size.height * 0.8f, center = Offset(size.width, 0f))
        }
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(48.dp).background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Memory, null, tint = Color.White) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(dump.displayName, style = MaterialTheme.typography.titleLarge, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        dump.uid.ifEmpty { "No UID" },
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        color = Color.White.copy(alpha = 0.9f),
                    )
                }
                if (saved) Icon(Icons.Rounded.BookmarkAdded, "Saved", tint = Color.White)
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(dump.family.label)
                Chip("${dump.totalBytes} bytes")
                Chip("${dump.readableBlocks} ${dump.family.unit}s")
            }
        }
    }
}

@Composable
private fun Chip(text: String) {
    Box(Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = Color.White)
    }
}

@Composable
private fun BlockRow(block: MemoryBlock, unit: String, onEdit: () -> Unit) {
    val color = roleColor(block.role)
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        onClick = onEdit,
        enabled = block.writable && block.readable,
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(34.dp).background(color.copy(alpha = 0.14f), RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("${block.index}", style = MaterialTheme.typography.labelMedium, color = color)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                if (block.readable) {
                    SelectionContainer {
                        Text(
                            block.dataHex.chunked(2).joinToString(" "),
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else {
                    Text("Locked — custom key", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    (block.sector?.let { "Sector $it · " } ?: "") + roleLabel(block.role),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            if (!block.readable) {
                Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
            } else if (block.writable) {
                Icon(Icons.Rounded.Edit, "Edit", tint = color, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun HexEditorDialog(block: MemoryBlock, unit: String, onDismiss: () -> Unit, onWrite: (String) -> Unit) {
    val expected = block.sizeBytes
    var text by rememberSaveable { mutableStateOf(block.dataHex) }
    val clean = text.filter { !it.isWhitespace() }.uppercase()
    val valid = clean.length == expected * 2 && clean.all { it in "0123456789ABCDEF" }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Bolt, null, tint = roleColor(block.role)) },
        title = { Text("Edit $unit ${block.index}") },
        text = {
            Column {
                Text(
                    "$expected bytes · ${roleLabel(block.role)}. Editing this writes directly to the tag and can't be undone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (block.role == "sector-trailer") {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "⚠ This block holds the sector keys and access bits. A wrong value can lock the sector permanently.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Red,
                    )
                }
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Hex (${expected * 2} chars)") },
                    isError = clean.isNotEmpty() && !valid,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "${clean.length / 2} / $expected bytes",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (valid) Emerald else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        },
        confirmButton = { TextButton(enabled = valid && clean != block.dataHex.uppercase(), onClick = { onWrite(clean) }) { Text("Write") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun NameDialog(initial: String, title: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(name.trim()) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun report(dump: RfidDump): String = buildString {
    appendLine("RFID dump — ${dump.displayName}")
    appendLine("Scanned: ${formatDate(dump.scannedAt)}")
    appendLine("UID: ${dump.uid}")
    appendLine("Type: ${dump.tagType} (${dump.family.label})")
    appendLine("Memory: ${dump.totalBytes} bytes, ${dump.blockSize}-byte ${dump.family.unit}s")
    if (dump.keysUsed.isNotEmpty()) appendLine("Keys used: ${dump.keysUsed.joinToString(", ")}")
    appendLine()
    dump.blocks.forEach { b ->
        appendLine("[%3d] %s".format(b.index, if (b.readable) b.dataHex else "-- locked --"))
    }
}.trimEnd()
