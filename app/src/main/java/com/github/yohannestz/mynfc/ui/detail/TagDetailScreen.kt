package com.github.yohannestz.mynfc.ui.detail

import android.nfc.NdefMessage
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.yohannestz.mynfc.data.model.NdefRecordInfo
import com.github.yohannestz.mynfc.data.model.RecordKind
import com.github.yohannestz.mynfc.data.model.ScannedTag
import com.github.yohannestz.mynfc.nfc.NdefParser
import com.github.yohannestz.mynfc.nfc.NfcOperation
import com.github.yohannestz.mynfc.nfc.formatBytes
import com.github.yohannestz.mynfc.ui.common.LocalAppContainer
import com.github.yohannestz.mynfc.ui.common.LocalSnackbar
import com.github.yohannestz.mynfc.ui.common.color
import com.github.yohannestz.mynfc.ui.common.copyToClipboard
import com.github.yohannestz.mynfc.ui.common.fileStamp
import com.github.yohannestz.mynfc.ui.common.formatDate
import com.github.yohannestz.mynfc.ui.common.icon
import com.github.yohannestz.mynfc.ui.common.isActionable
import com.github.yohannestz.mynfc.ui.common.openRecord
import com.github.yohannestz.mynfc.ui.common.rememberJsonExporter
import com.github.yohannestz.mynfc.ui.common.shareText
import com.github.yohannestz.mynfc.ui.components.CircleIconButton
import com.github.yohannestz.mynfc.ui.components.EmptyState
import com.github.yohannestz.mynfc.ui.components.IconBadge
import com.github.yohannestz.mynfc.ui.components.InfoCard
import com.github.yohannestz.mynfc.ui.components.InfoRow
import com.github.yohannestz.mynfc.ui.components.PrimaryPillButton
import com.github.yohannestz.mynfc.ui.components.ScreenHeader
import com.github.yohannestz.mynfc.ui.components.SecondaryPillButton
import com.github.yohannestz.mynfc.ui.components.SectionLabel
import com.github.yohannestz.mynfc.ui.components.SegmentedToggle
import com.github.yohannestz.mynfc.ui.theme.Emerald
import com.github.yohannestz.mynfc.ui.theme.Gradients
import com.github.yohannestz.mynfc.ui.theme.Red
import kotlinx.coroutines.launch

@Composable
fun TagDetailScreen(id: String, onClose: () -> Unit) {
    val container = LocalAppContainer.current
    val repository = container.repository
    val savedTags by repository.savedTags.collectAsStateWithLifecycle()
    val recent by repository.recent.collectAsStateWithLifecycle()
    val savedTag = savedTags.firstOrNull { it.id == id }
    val tag = savedTag ?: recent[id]
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val exporter = rememberJsonExporter { ok ->
        scope.launch { snackbar.showSnackbar(if (ok) "Exported to JSON" else "Export failed") }
    }
    fun toast(msg: String) = scope.launch { snackbar.showSnackbar(msg) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        ScreenHeader(
            title = "NFC Details",
            subtitle = tag?.let { formatDate(it.scannedAt) },
            navigation = { CircleIconButton(Icons.Rounded.Close, "Close", onClose) },
            action = {
                if (savedTag != null) {
                    CircleIconButton(Icons.Rounded.DeleteOutline, "Delete", { showDeleteDialog = true }, tint = Red)
                } else if (tag != null) {
                    CircleIconButton(Icons.Rounded.Share, "Share", { context.shareText("NFC tag ${tag.uid}", report(tag)) })
                }
            },
        )

        if (tag == null) {
            EmptyState(Icons.Rounded.Nfc, "Tag not found", "This scan is no longer available.", Modifier.weight(1f))
            return@Column
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            HeroCard(tag, saved = savedTag != null)
            Spacer(Modifier.height(16.dp))
            SegmentedToggle(listOf("Simple", "Advanced"), tab, { tab = it })

            AnimatedContent(tab, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "tab") { t ->
                Column {
                    if (t == 0) SimpleContent(tag, onCopy = { label, text ->
                        context.copyToClipboard(label, text)
                        toast("$label copied")
                    })
                    else AdvancedContent(tag)
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // Sticky action area
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (tab == 0) {
                    if (savedTag == null) {
                        PrimaryPillButton("Save NFC Tag", { showSaveDialog = true }, Modifier.fillMaxWidth(), icon = Icons.Rounded.BookmarkAdd)
                    } else {
                        PrimaryPillButton("Rename", { showSaveDialog = true }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Edit, containerColor = Emerald)
                    }
                    SecondaryPillButton(
                        "Write to another tag",
                        onClick = {
                            if (!tag.hasNdefData) toast("This tag has no data to copy")
                            else container.nfcController.start(
                                NfcOperation.Write(
                                    NdefMessage(tag.records.map(NdefParser::toRecord).toTypedArray()),
                                    title = "Copy to tag",
                                ),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Rounded.Nfc,
                    )
                } else {
                    PrimaryPillButton("Copy All", {
                        context.copyToClipboard("NFC tag", report(tag))
                        toast("Tag details copied")
                    }, Modifier.fillMaxWidth(), icon = Icons.Rounded.ContentCopy)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SecondaryPillButton("Share", { context.shareText("NFC tag ${tag.uid}", report(tag)) }, Modifier.weight(1f), icon = Icons.Rounded.Share)
                        SecondaryPillButton(
                            "Export JSON",
                            {
                                exporter.export("nfc-tag-${tag.uid.replace(":", "")}-${fileStamp()}.json") {
                                    repository.exportJson(tags = listOf(tag), records = emptyList())
                                }
                            },
                            Modifier.weight(1f),
                            icon = Icons.Rounded.DataObject,
                            outlined = true,
                        )
                    }
                }
            }
        }
    }

    if (showSaveDialog && tag != null) {
        NameDialog(
            initial = tag.name ?: tag.tagType,
            title = if (savedTag == null) "Save tag" else "Rename tag",
            onDismiss = { showSaveDialog = false },
            onConfirm = { name ->
                repository.saveTag(tag.copy(name = name.ifBlank { null }))
                showSaveDialog = false
                toast(if (savedTag == null) "Tag saved" else "Tag renamed")
            },
        )
    }
    if (showDeleteDialog && savedTag != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete saved tag?") },
            text = { Text("\"${savedTag.displayName}\" will be removed from your library.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    repository.deleteTag(savedTag.id)
                    onClose()
                }) { Text("Delete", color = Red) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroCard(tag: ScannedTag, saved: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Gradients.read),
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(alpha = 0.10f), radius = size.height * 0.8f, center = Offset(size.width, 0f))
        }
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Nfc, null, tint = Color.White) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(tag.displayName, style = MaterialTheme.typography.titleLarge, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        tag.uid.ifEmpty { "No UID" },
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        color = Color.White.copy(alpha = 0.9f),
                    )
                }
                if (saved) Icon(Icons.Rounded.BookmarkAdded, "Saved", tint = Color.White)
            }
            Spacer(Modifier.height(14.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (tag.name != null) Chip(tag.tagType)
                tag.ndefType?.let { Chip(it) }
                Chip(
                    when (tag.writable) {
                        true -> "Writable"
                        false -> "Read-only"
                        null -> if (tag.formatable) "Formatable" else "Not NDEF"
                    },
                )
                Chip("${tag.records.count { it.kind != RecordKind.EMPTY }} record(s)")
            }
            val max = tag.maxSize
            if (max != null && max > 0) {
                Spacer(Modifier.height(16.dp))
                val used = tag.usedSize ?: 0
                Row {
                    Text("Memory", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.9f), modifier = Modifier.weight(1f))
                    Text("$used / ${formatBytes(max)}", style = MaterialTheme.typography.labelMedium, color = Color.White)
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (used.toFloat() / max).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.25f),
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
            }
        }
    }
}

@Composable
private fun Chip(text: String) {
    Box(
        Modifier
            .background(Color.White.copy(alpha = 0.2f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = Color.White)
    }
}

@Composable
private fun SimpleContent(tag: ScannedTag, onCopy: (String, String) -> Unit) {
    val context = LocalContext.current
    SectionLabel("NDEF content")
    val records = tag.records.filter { it.kind != RecordKind.EMPTY }
    if (records.isEmpty()) {
        InfoCard {
            Column(Modifier.padding(vertical = 14.dp)) {
                Text("Empty NFC Tag", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (tag.supportsNdef || tag.formatable) "This tag has no stored data" else "This tag doesn't use NDEF",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            records.forEach { record ->
                RecordCard(
                    record,
                    onOpen = { context.openRecord(record) },
                    onCopy = { onCopy(record.kind.label, record.value) },
                )
            }
        }
    }

    SectionLabel("Tag information")
    InfoCard {
        InfoRow("Type", tag.tagType)
        InfoRow("Technologies", tag.technologies.joinToString(", "))
        InfoRow("Manufacturer", tag.manufacturer ?: "—")
        InfoRow("Memory", tag.maxSize?.let { "${tag.usedSize ?: 0} / ${formatBytes(it)}" } ?: "—")
        InfoRow("UID", tag.uid.ifEmpty { "—" }, showDivider = false, monospace = true) {
            IconButton(onClick = { onCopy("UID", tag.uid) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Rounded.ContentCopy, "Copy UID", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
        }
    }

    SectionLabel("Status")
    InfoCard {
        StatusRow("Writable", tag.writable)
        StatusRow("Can be made read-only", tag.canMakeReadOnly)
        StatusRow("NDEF formatable", tag.formatable, last = true)
    }
}

@Composable
private fun StatusRow(label: String, value: Boolean?, last: Boolean = false) {
    InfoRow(
        label,
        when (value) { true -> "Yes"; false -> "No"; null -> "—" },
        showDivider = !last,
        valueColor = when (value) {
            true -> Emerald
            false -> MaterialTheme.colorScheme.onSurface
            null -> MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}

@Composable
private fun RecordCard(record: NdefRecordInfo, onOpen: () -> Unit, onCopy: () -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(record.kind.icon, record.kind.color)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(record.kind.label, style = MaterialTheme.typography.titleSmall)
                    Text(
                        record.mimeType ?: record.tnfName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(formatBytes(record.sizeBytes), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            SelectionContainer {
                Text(record.value, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onCopy) {
                    Icon(Icons.Rounded.ContentCopy, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copy")
                }
                if (record.isActionable) {
                    TextButton(onClick = onOpen) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Open")
                    }
                }
            }
        }
    }
}

@Composable
private fun AdvancedContent(tag: ScannedTag) {
    if (tag.chipInfo.isNotEmpty()) {
        SectionLabel("Chip information")
        InfoCard {
            tag.chipInfo.forEachIndexed { i, entry ->
                InfoRow(entry.label, entry.value, showDivider = i != tag.chipInfo.lastIndex, monospace = entry.value.startsWith("0x") || entry.value.contains(' '))
            }
        }
    }

    SectionLabel("NDEF")
    InfoCard {
        InfoRow("NDEF type", tag.ndefType ?: "Not NDEF")
        InfoRow("Max size", tag.maxSize?.let(::formatBytes) ?: "—")
        InfoRow("Used", tag.usedSize?.let(::formatBytes) ?: "—")
        InfoRow("Records", "${tag.records.size}", showDivider = false)
    }

    tag.records.forEachIndexed { index, record ->
        SectionLabel("Record #${index + 1}")
        InfoCard {
            InfoRow("TNF", "${record.tnfName} (${record.tnf})")
            InfoRow("Type", record.type.ifEmpty { "—" }, monospace = true)
            if (record.idHex.isNotEmpty()) InfoRow("ID", record.idHex, monospace = true)
            InfoRow("Size", formatBytes(record.sizeBytes))
            Column(Modifier.padding(vertical = 14.dp)) {
                Text("Payload (hex)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                SelectionContainer {
                    Text(
                        record.payloadHex.chunked(2).joinToString(" ").ifEmpty { "—" },
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                            .padding(12.dp),
                    )
                }
            }
        }
    }

    SectionLabel("Technologies")
    InfoCard {
        tag.technologies.forEachIndexed { i, tech ->
            Text(
                "android.nfc.tech.$tech",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.padding(vertical = 12.dp),
            )
            if (i != tag.technologies.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
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

/** Human-readable dump used for Copy All / Share. */
private fun report(tag: ScannedTag): String = buildString {
    appendLine("NFC Tag — ${tag.displayName}")
    appendLine("Scanned: ${formatDate(tag.scannedAt)}")
    appendLine("UID: ${tag.uid}")
    appendLine("Type: ${tag.tagType}")
    appendLine("Technologies: ${tag.technologies.joinToString(", ")}")
    tag.manufacturer?.let { appendLine("Manufacturer: $it") }
    tag.ndefType?.let { appendLine("NDEF type: $it") }
    tag.maxSize?.let { appendLine("Memory: ${tag.usedSize ?: 0} / $it bytes") }
    tag.writable?.let { appendLine("Writable: ${if (it) "Yes" else "No"}") }
    if (tag.chipInfo.isNotEmpty()) {
        appendLine()
        appendLine("Chip")
        tag.chipInfo.forEach { appendLine("  ${it.label}: ${it.value}") }
    }
    if (tag.records.isNotEmpty()) {
        appendLine()
        appendLine("Records")
        tag.records.forEachIndexed { i, r -> appendLine("  #${i + 1} ${r.kind.label}: ${r.value.replace("\n", " | ")}") }
    }
}.trimEnd()
