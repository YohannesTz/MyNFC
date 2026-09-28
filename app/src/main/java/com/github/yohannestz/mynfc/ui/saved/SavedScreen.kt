package com.github.yohannestz.mynfc.ui.saved

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.yohannestz.mynfc.data.model.RecordKind
import com.github.yohannestz.mynfc.data.model.ScannedTag
import com.github.yohannestz.mynfc.ui.common.LocalAppContainer
import com.github.yohannestz.mynfc.ui.common.LocalSnackbar
import com.github.yohannestz.mynfc.ui.common.color
import com.github.yohannestz.mynfc.ui.common.fileStamp
import com.github.yohannestz.mynfc.ui.common.icon
import com.github.yohannestz.mynfc.ui.common.relativeTime
import com.github.yohannestz.mynfc.ui.common.rememberJsonExporter
import com.github.yohannestz.mynfc.ui.common.rememberJsonImporter
import com.github.yohannestz.mynfc.ui.components.CircleIconButton
import com.github.yohannestz.mynfc.ui.components.EmptyState
import com.github.yohannestz.mynfc.ui.components.IconBadge
import com.github.yohannestz.mynfc.ui.components.PrimaryPillButton
import com.github.yohannestz.mynfc.ui.components.ScreenHeader
import com.github.yohannestz.mynfc.ui.theme.Blue
import kotlinx.coroutines.launch

@Composable
fun SavedScreen(onOpen: (String) -> Unit) {
    val repository = LocalAppContainer.current.repository
    val tags by repository.savedTags.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }

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
    val exportAll = {
        exporter.export("mynfc-export-${fileStamp()}.json") { repository.exportJson() }
    }

    val filtered = remember(tags, query) {
        val q = query.trim()
        if (q.isEmpty()) tags
        else tags.filter { t ->
            t.displayName.contains(q, true) || t.uid.contains(q, true) || t.tagType.contains(q, true) ||
                t.records.any { it.value.contains(q, true) }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        ScreenHeader(
            title = "Saved Tags",
            subtitle = "${tags.size} tag(s)",
            action = {
                Box {
                    CircleIconButton(Icons.Rounded.MoreVert, "More", { menu = true })
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Export all to JSON") },
                            leadingIcon = { Icon(Icons.Rounded.FileDownload, null) },
                            onClick = { menu = false; exportAll() },
                        )
                        DropdownMenuItem(
                            text = { Text("Import from JSON") },
                            leadingIcon = { Icon(Icons.Rounded.FileUpload, null) },
                            onClick = { menu = false; importer() },
                        )
                    }
                }
            },
        )

        if (tags.isEmpty()) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                EmptyState(
                    Icons.Rounded.Bookmarks,
                    "No saved tags",
                    "Scan a tag and tap \"Save NFC Tag\" to keep it here. You can also import a JSON export.",
                ) {
                    PrimaryPillButton("Import JSON", importer, icon = Icons.Rounded.FileUpload)
                }
            }
            return@Column
        }

        TextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search name, UID or content") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = if (query.isNotEmpty()) {
                { IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "Clear") } }
            } else null,
            singleLine = true,
            shape = CircleShape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(filtered, key = { it.id }) { tag ->
                SavedTagCard(tag, Modifier.animateItem()) { onOpen(tag.id) }
            }
            item {
                Surface(
                    onClick = exportAll,
                    shape = MaterialTheme.shapes.large,
                    color = Blue.copy(alpha = 0.10f),
                    modifier = Modifier.padding(top = 6.dp),
                ) {
                    Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.DataObject, null, tint = Blue)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Export library", style = MaterialTheme.typography.titleSmall, color = Blue)
                            Text(
                                "All saved tags and records in one JSON file",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedTagCard(tag: ScannedTag, modifier: Modifier, onClick: () -> Unit) {
    val primary = tag.records.firstOrNull { it.kind != RecordKind.EMPTY }
    Surface(onClick = onClick, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, modifier = modifier) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (primary != null) IconBadge(primary.kind.icon, primary.kind.color, size = 48.dp)
            else IconBadge(Icons.Rounded.Nfc, Blue, size = 48.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        tag.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(relativeTime(tag.scannedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
                Text(
                    primary?.value?.lineSequence()?.first() ?: "Empty tag",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    tag.uid,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = MaterialTheme.typography.bodySmall.letterSpacing),
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                )
            }
        }
    }
}
