package com.github.yohannestz.mynfc.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Contactless
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.PhonelinkRing
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.yohannestz.mynfc.data.model.ScannedTag
import com.github.yohannestz.mynfc.nfc.NfcStatus
import com.github.yohannestz.mynfc.ui.common.LocalAppContainer
import com.github.yohannestz.mynfc.ui.common.openNfcSettings
import com.github.yohannestz.mynfc.ui.common.rememberNfcStatus
import com.github.yohannestz.mynfc.ui.common.relativeTime
import com.github.yohannestz.mynfc.ui.components.GradientActionCard
import com.github.yohannestz.mynfc.ui.components.IconBadge
import com.github.yohannestz.mynfc.ui.components.InfoCard
import com.github.yohannestz.mynfc.ui.components.SectionLabel
import com.github.yohannestz.mynfc.ui.theme.Blue
import com.github.yohannestz.mynfc.ui.theme.Emerald
import com.github.yohannestz.mynfc.ui.theme.Gradients
import com.github.yohannestz.mynfc.ui.theme.Orange
import com.github.yohannestz.mynfc.ui.theme.Red
import java.util.Calendar

@Composable
fun HomeScreen(
    onRead: () -> Unit,
    onWrite: () -> Unit,
    onOpenTag: (String) -> Unit,
    onSeeSaved: () -> Unit,
) {
    val container = LocalAppContainer.current
    val saved by container.repository.savedTags.collectAsStateWithLifecycle()
    val records by container.repository.records.collectAsStateWithLifecycle()
    val status = rememberNfcStatus()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Header(status) }

        if (status != NfcStatus.READY) {
            item {
                StatusBanner(status) { context.openNfcSettings() }
            }
        }

        item {
            GradientActionCard(
                title = "Read NFC",
                description = "Scan a tag to see its data, chip details and memory — then save or export it.",
                icon = Icons.Rounded.Contactless,
                brush = Gradients.read,
                onClick = onRead,
            )
        }
        item {
            GradientActionCard(
                title = "Write NFC",
                description = "Put links, contacts, Wi-Fi and more on a tag so anyone can open it with a tap.",
                icon = Icons.Rounded.EditNote,
                brush = Gradients.write,
                onClick = onWrite,
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                StatCard("Saved tags", saved.size, Icons.Rounded.Bookmarks, Emerald, Modifier.weight(1f), onSeeSaved)
                StatCard("My records", records.size, Icons.Rounded.EditNote, Orange, Modifier.weight(1f), onWrite)
            }
        }

        if (saved.isNotEmpty()) {
            item {
                SectionLabel("Recently saved") {
                    TextButton(onClick = onSeeSaved) { Text("See all") }
                }
            }
            items(saved.take(3), key = { it.id }) { tag -> RecentTagRow(tag) { onOpenTag(tag.id) } }
        } else {
            item { HowItWorks() }
        }
    }
}

@Composable
private fun Header(status: NfcStatus) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                greeting(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("MyNFC", style = MaterialTheme.typography.headlineMedium)
        }
        val (label, color) = when (status) {
            NfcStatus.READY -> "NFC on" to Emerald
            NfcStatus.DISABLED -> "NFC off" to Orange
            NfcStatus.UNSUPPORTED -> "No NFC" to Red
        }
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(color, CircleShape),
                )
                Spacer(Modifier.width(8.dp))
                Text(label, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun StatusBanner(status: NfcStatus, onOpenSettings: () -> Unit) {
    val unsupported = status == NfcStatus.UNSUPPORTED
    Surface(
        shape = MaterialTheme.shapes.large,
        color = (if (unsupported) Red else Orange).copy(alpha = 0.12f),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.WarningAmber, null, tint = if (unsupported) Red else Orange)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (unsupported) "NFC not available" else "NFC is turned off",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    if (unsupported) "This device has no NFC hardware." else "Turn it on to read and write tags.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!unsupported) TextButton(onClick = onOpenSettings) { Text("Turn on") }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    count: Int,
    icon: ImageVector,
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, onClick = onClick, modifier = modifier) {
        Column(Modifier.padding(18.dp)) {
            IconBadge(icon, color)
            Spacer(Modifier.height(14.dp))
            Text("$count", style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RecentTagRow(tag: ScannedTag, onClick: () -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, onClick = onClick) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.Nfc, Blue, size = 44.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(tag.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    tag.uid,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Text(relativeTime(tag.scannedAt), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HowItWorks() {
    Column {
        SectionLabel("How it works")
        InfoCard {
            Step(1, Icons.Rounded.PhonelinkRing, "Tap Read NFC", "Hold a tag against the back of your phone, near the camera.")
            Step(2, Icons.Rounded.Bookmarks, "Save or export", "Keep tags in your library and export everything as JSON.")
            Step(3, Icons.Rounded.EditNote, "Write your own", "Compose links, contacts or Wi-Fi and write them to any NDEF tag.", last = true)
        }
    }
}

@Composable
private fun Step(n: Int, icon: ImageVector, title: String, body: String, last: Boolean = false) {
    Row(Modifier.padding(vertical = 14.dp)) {
        Box(
            Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("$n", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (!last) androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}
