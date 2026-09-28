package com.github.yohannestz.mynfc.ui.write

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.yohannestz.mynfc.data.model.RecordCategory
import com.github.yohannestz.mynfc.data.model.RecordType
import com.github.yohannestz.mynfc.data.model.WriteRecord
import com.github.yohannestz.mynfc.nfc.NfcOperation
import com.github.yohannestz.mynfc.nfc.RecordFactory
import com.github.yohannestz.mynfc.nfc.formatBytes
import com.github.yohannestz.mynfc.ui.common.LocalAppContainer
import com.github.yohannestz.mynfc.ui.common.color
import com.github.yohannestz.mynfc.ui.common.icon
import com.github.yohannestz.mynfc.ui.common.relativeTime
import com.github.yohannestz.mynfc.ui.components.CircleIconButton
import com.github.yohannestz.mynfc.ui.components.EmptyState
import com.github.yohannestz.mynfc.ui.components.IconBadge
import com.github.yohannestz.mynfc.ui.components.InfoCard
import com.github.yohannestz.mynfc.ui.components.PrimaryPillButton
import com.github.yohannestz.mynfc.ui.components.ScreenHeader
import com.github.yohannestz.mynfc.ui.components.SectionLabel
import com.github.yohannestz.mynfc.ui.components.SegmentedToggle
import com.github.yohannestz.mynfc.ui.theme.Orange
import com.github.yohannestz.mynfc.ui.theme.Red

@Composable
fun WriteScreen(onCreate: (RecordType) -> Unit, onEdit: (WriteRecord) -> Unit) {
    val container = LocalAppContainer.current
    val records by container.repository.records.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(if (records.isEmpty()) 1 else 0) }
    var selection by remember { mutableStateOf(setOf<String>()) }
    var pendingDelete by remember { mutableStateOf<Set<String>>(emptySet()) }
    var knownCount by rememberSaveable { mutableIntStateOf(records.size) }
    // Coming back from the editor with a new record: show it in My Records.
    LaunchedEffect(records.size) {
        if (records.size > knownCount) tab = 0
        knownCount = records.size
    }

    fun write(list: List<WriteRecord>) {
        val title = if (list.size == 1) "Write ${list[0].type.title}" else "Write ${list.size} records"
        container.nfcController.start(NfcOperation.Write(RecordFactory.toMessage(list), title = title))
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        ScreenHeader(
            title = if (selection.isEmpty()) "Write NFC" else "${selection.size} selected",
            navigation = if (selection.isNotEmpty()) {
                { CircleIconButton(Icons.Rounded.Close, "Clear selection", { selection = emptySet() }) }
            } else null,
            action = if (selection.isNotEmpty()) {
                { CircleIconButton(Icons.Rounded.DeleteOutline, "Delete", { pendingDelete = selection }, tint = Red) }
            } else null,
        )
        SegmentedToggle(
            listOf("My Records", "Add New"),
            tab,
            { tab = it; selection = emptySet() },
            Modifier.padding(horizontal = 16.dp),
        )

        Box(Modifier.weight(1f)) {
            if (tab == 0) {
                if (records.isEmpty()) {
                    EmptyState(
                        Icons.Rounded.EditNote,
                        "No Records Yet",
                        "Add data like a website, Wi-Fi, or contact to write to your tag.",
                        Modifier.align(Alignment.Center),
                    ) {
                        PrimaryPillButton("Add New Record", { tab = 1 }, icon = Icons.Rounded.Add)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item {
                            Text(
                                "Tip: long-press to select several records and write them to one tag.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                            )
                        }
                        items(records, key = { it.id }) { record ->
                            val selected = record.id in selection
                            RecordRow(
                                record = record,
                                selecting = selection.isNotEmpty(),
                                selected = selected,
                                onClick = {
                                    if (selection.isNotEmpty()) selection = if (selected) selection - record.id else selection + record.id
                                    else write(listOf(record))
                                },
                                onLongClick = { selection = selection + record.id },
                                onEdit = { onEdit(record) },
                                onDelete = { pendingDelete = setOf(record.id) },
                                onWrite = { write(listOf(record)) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            } else {
                AddNewList(onCreate)
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = selection.isNotEmpty(),
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                val chosen = records.filter { it.id in selection }
                val size = RecordFactory.sizeOf(chosen)
                Surface(color = MaterialTheme.colorScheme.background) {
                    PrimaryPillButton(
                        "Write ${chosen.size} record(s) · ${formatBytes(size)}",
                        { write(chosen); selection = emptySet() },
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        icon = Icons.Rounded.Nfc,
                        containerColor = Orange,
                    )
                }
            }
        }
    }

    if (pendingDelete.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { pendingDelete = emptySet() },
            title = { Text(if (pendingDelete.size == 1) "Delete record?" else "Delete ${pendingDelete.size} records?") },
            text = { Text("This only removes them from the app, not from any tag.") },
            confirmButton = {
                TextButton(onClick = {
                    container.repository.deleteRecords(pendingDelete)
                    selection = selection - pendingDelete
                    pendingDelete = emptySet()
                }) { Text("Delete", color = Red) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = emptySet() }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecordRow(
    record: WriteRecord,
    selecting: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onWrite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val border by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, label = "border")
    var menu by remember { mutableStateOf(false) }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .border(2.dp, border, MaterialTheme.shapes.large),
    ) {
        Row(
            Modifier
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selecting) {
                Icon(
                    if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    null,
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                )
                Spacer(Modifier.width(12.dp))
            }
            IconBadge(record.type.icon, record.type.color, size = 44.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(record.type.title, style = MaterialTheme.typography.titleSmall)
                Text(
                    RecordFactory.summary(record),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${formatBytes(RecordFactory.sizeOf(listOf(record)))} · ${relativeTime(record.updatedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            if (!selecting) {
                FilledTonalButton(onClick = onWrite, contentPadding = PaddingValues(horizontal = 14.dp)) {
                    Icon(Icons.Rounded.Nfc, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Write")
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "More") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                            onClick = { menu = false; onEdit() },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = Red) },
                            leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null, tint = Red) },
                            onClick = { menu = false; onDelete() },
                        )
                    }
                }
            } else {
                Spacer(Modifier.width(12.dp))
            }
        }
    }
}

@Composable
private fun AddNewList(onCreate: (RecordType) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
        RecordCategory.entries.forEach { category ->
            val types = RecordType.entries.filter { it.category == category }
            item(key = category.name) {
                SectionLabel(category.title)
                InfoCard {
                    types.forEachIndexed { i, type ->
                        TypeRow(type) { onCreate(type) }
                        if (i != types.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun TypeRow(type: RecordType, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(type.icon, type.color)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(type.title, style = MaterialTheme.typography.titleSmall)
                Text(type.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
