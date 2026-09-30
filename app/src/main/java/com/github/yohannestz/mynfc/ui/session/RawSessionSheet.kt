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
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Memory
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.yohannestz.mynfc.nfc.RawOperation
import com.github.yohannestz.mynfc.nfc.RawSessionState
import com.github.yohannestz.mynfc.ui.components.PrimaryPillButton
import com.github.yohannestz.mynfc.ui.components.ScanPulse
import com.github.yohannestz.mynfc.ui.components.SecondaryPillButton
import com.github.yohannestz.mynfc.ui.theme.Emerald
import com.github.yohannestz.mynfc.ui.theme.Indigo
import com.github.yohannestz.mynfc.ui.theme.Red

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RawSessionSheet(
    state: RawSessionState,
    onDismiss: () -> Unit,
    onRetry: (RawOperation) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(state) {
        if (state is RawSessionState.Success || state is RawSessionState.Failed) {
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
            label = "rawSession",
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
                    is RawSessionState.Waiting -> {
                        Text(s.operation.title, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        ScanPulse(color = Indigo, size = 220.dp, icon = Icons.Rounded.Memory)
                        Text("Ready", style = MaterialTheme.typography.titleMedium)
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

                    is RawSessionState.Working -> {
                        Spacer(Modifier.height(40.dp))
                        if (s.progress > 0f) {
                            CircularProgressIndicator(progress = { s.progress }, modifier = Modifier.size(72.dp), strokeWidth = 6.dp, color = Indigo)
                        } else {
                            CircularProgressIndicator(Modifier.size(72.dp), strokeWidth = 6.dp, color = Indigo)
                        }
                        Spacer(Modifier.height(28.dp))
                        Text("Writing…", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (s.progress > 0f) "${(s.progress * 100).toInt()}% — keep the tag still" else "Keep the tag against your phone",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(60.dp))
                    }

                    is RawSessionState.Success -> ResultBlock(
                        Icons.Rounded.Check, Emerald, s.message, "The tag's memory has been updated.",
                    ) { PrimaryPillButton("Done", onDismiss, Modifier.fillMaxWidth(), containerColor = Emerald) }

                    is RawSessionState.Failed -> ResultBlock(
                        Icons.Rounded.ErrorOutline, Red, "Write failed", s.message + "\nHold the tag again to retry.",
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SecondaryPillButton("Close", onDismiss, Modifier.weight(1f))
                            PrimaryPillButton("Retry", { onRetry(s.operation) }, Modifier.weight(1f))
                        }
                    }

                    RawSessionState.Idle -> Spacer(Modifier.height(1.dp))
                }
            }
        }
    }
}

@Composable
private fun ResultBlock(icon: ImageVector, color: Color, title: String, message: String, actions: @Composable () -> Unit) {
    Spacer(Modifier.height(24.dp))
    AnimatedContent(targetState = icon, transitionSpec = {
        scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) togetherWith fadeOut()
    }, label = "icon") { i ->
        Box(
            Modifier.size(112.dp).background(color.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(76.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
                Icon(i, null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
        }
    }
    Spacer(Modifier.height(24.dp))
    Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
    Spacer(Modifier.height(6.dp))
    Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    Spacer(Modifier.height(28.dp))
    actions()
}
