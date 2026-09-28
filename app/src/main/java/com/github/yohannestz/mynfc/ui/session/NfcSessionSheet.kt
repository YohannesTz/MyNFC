package com.github.yohannestz.mynfc.ui.session

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.yohannestz.mynfc.nfc.NfcOperation
import com.github.yohannestz.mynfc.nfc.SessionState
import com.github.yohannestz.mynfc.ui.components.PrimaryPillButton
import com.github.yohannestz.mynfc.ui.components.ScanPulse
import com.github.yohannestz.mynfc.ui.components.SecondaryPillButton
import com.github.yohannestz.mynfc.ui.theme.Emerald
import com.github.yohannestz.mynfc.ui.theme.Orange
import com.github.yohannestz.mynfc.ui.theme.Pink
import com.github.yohannestz.mynfc.ui.theme.Purple
import com.github.yohannestz.mynfc.ui.theme.Red

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcSessionSheet(
    state: SessionState,
    onDismiss: () -> Unit,
    onRetry: (NfcOperation) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(state) {
        if (state is SessionState.Success || state is SessionState.Failed) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        AnimatedContent(
            targetState = state,
            contentKey = { it::class },
            transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.92f)) togetherWith fadeOut() },
            label = "session",
        ) { s ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (s) {
                    is SessionState.Waiting -> {
                        Text(s.operation.title, style = MaterialTheme.typography.titleLarge)
                        if (s.operation == NfcOperation.Clone) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Step ${s.step + 1} of 2",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        ScanPulse(color = accentFor(s.operation), size = 220.dp, icon = iconFor(s.operation))
                        Text(
                            "Ready to ${verbFor(s.operation)}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            s.hint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(24.dp))
                        SecondaryPillButton("Cancel", onDismiss, Modifier.fillMaxWidth())
                    }

                    is SessionState.Working -> {
                        Spacer(Modifier.height(40.dp))
                        CircularProgressIndicator(Modifier.size(72.dp), strokeWidth = 6.dp, color = accentFor(s.operation))
                        Spacer(Modifier.height(28.dp))
                        Text("Hold still…", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Keep the tag against your phone",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(60.dp))
                    }

                    is SessionState.Success -> ResultContent(
                        icon = Icons.Rounded.Check,
                        color = Emerald,
                        title = s.message,
                        message = "You can now tap the tag with any NFC phone.",
                    ) {
                        PrimaryPillButton("Done", onDismiss, Modifier.fillMaxWidth(), containerColor = Emerald)
                    }

                    is SessionState.Failed -> ResultContent(
                        icon = Icons.Rounded.ErrorOutline,
                        color = Red,
                        title = "Something went wrong",
                        message = s.message + "\nTap the tag again to retry.",
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SecondaryPillButton("Close", onDismiss, Modifier.weight(1f))
                            PrimaryPillButton("Start over", { onRetry(s.operation) }, Modifier.weight(1f))
                        }
                    }

                    SessionState.Idle -> Spacer(Modifier.height(1.dp))
                }
            }
        }
    }
}

@Composable
private fun ResultContent(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    title: String,
    message: String,
    actions: @Composable () -> Unit,
) {
    Spacer(Modifier.height(24.dp))
    AnimatedContent(targetState = icon, transitionSpec = {
        scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) togetherWith fadeOut()
    }, label = "resultIcon") { i ->
        Box(
            Modifier
                .size(112.dp)
                .background(color.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(76.dp)
                    .background(color, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(i, null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
        }
    }
    Spacer(Modifier.height(24.dp))
    Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
    Spacer(Modifier.height(6.dp))
    Text(
        message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(28.dp))
    actions()
}

@Composable
private fun accentFor(op: NfcOperation): Color = when (op) {
    is NfcOperation.Write -> Orange
    NfcOperation.Erase, NfcOperation.Format -> Pink
    NfcOperation.Lock -> Red
    NfcOperation.Clone -> Purple
}

private fun iconFor(op: NfcOperation) = when (op) {
    is NfcOperation.Write -> Icons.Rounded.Nfc
    NfcOperation.Erase -> Icons.Rounded.DeleteSweep
    NfcOperation.Format -> Icons.Rounded.RestartAlt
    NfcOperation.Lock -> Icons.Rounded.Lock
    NfcOperation.Clone -> Icons.Rounded.ContentCopy
}

private fun verbFor(op: NfcOperation) = when (op) {
    is NfcOperation.Write -> "write"
    NfcOperation.Erase -> "erase"
    NfcOperation.Format -> "format"
    NfcOperation.Lock -> "lock"
    NfcOperation.Clone -> "copy"
}
