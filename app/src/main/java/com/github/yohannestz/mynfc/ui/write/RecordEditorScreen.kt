package com.github.yohannestz.mynfc.ui.write

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.yohannestz.mynfc.data.TagRepository
import com.github.yohannestz.mynfc.data.model.FieldInput
import com.github.yohannestz.mynfc.data.model.FieldSpec
import com.github.yohannestz.mynfc.data.model.RecordType
import com.github.yohannestz.mynfc.data.model.WriteRecord
import com.github.yohannestz.mynfc.nfc.NfcController
import com.github.yohannestz.mynfc.nfc.NfcOperation
import com.github.yohannestz.mynfc.nfc.RecordFactory
import com.github.yohannestz.mynfc.nfc.formatBytes
import com.github.yohannestz.mynfc.ui.common.LocalAppContainer
import com.github.yohannestz.mynfc.ui.common.color
import com.github.yohannestz.mynfc.ui.common.formatDate
import com.github.yohannestz.mynfc.ui.common.icon
import com.github.yohannestz.mynfc.ui.components.CircleIconButton
import com.github.yohannestz.mynfc.ui.components.IconBadge
import com.github.yohannestz.mynfc.ui.components.InfoCard
import com.github.yohannestz.mynfc.ui.components.PrimaryPillButton
import com.github.yohannestz.mynfc.ui.components.ScreenHeader
import com.github.yohannestz.mynfc.ui.components.SectionLabel
import com.github.yohannestz.mynfc.ui.theme.Orange
import com.github.yohannestz.mynfc.ui.theme.Red
import java.util.UUID

class RecordEditorViewModel(
    private val repository: TagRepository,
    private val controller: NfcController,
    val type: RecordType,
    recordId: String?,
) : ViewModel() {
    private val existing = recordId?.let { id -> repository.records.value.firstOrNull { it.id == id } }
    val createdAt = existing?.createdAt ?: System.currentTimeMillis()
    val isEditing = existing != null
    val fields = mutableStateMapOf<String, String>().apply { putAll(existing?.fields ?: type.defaults()) }
    var showErrors by mutableStateOf(false)

    val error: String? get() = RecordFactory.validate(type, fields)

    val estimatedSize: Int get() = if (error == null) RecordFactory.sizeOf(listOf(build())) else 0

    private fun build() = WriteRecord(
        id = existing?.id ?: UUID.randomUUID().toString(),
        type = type,
        fields = fields.filterValues { it.isNotBlank() }.mapValues { it.value.trim() },
        createdAt = createdAt,
        updatedAt = System.currentTimeMillis(),
    )

    /** Returns true when saved; otherwise flips on inline errors. */
    fun save(thenWrite: Boolean): Boolean {
        if (error != null) {
            showErrors = true
            return false
        }
        val record = build()
        repository.upsertRecord(record)
        if (thenWrite) {
            controller.start(NfcOperation.Write(RecordFactory.toMessage(listOf(record)), title = "Write ${type.title}"))
        }
        return true
    }
}

@Composable
fun RecordEditorScreen(type: RecordType, recordId: String?, onClose: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: RecordEditorViewModel = viewModel(key = "editor-$type-$recordId") {
        RecordEditorViewModel(container.repository, container.nfcController, type, recordId)
    }
    val clipboard = LocalClipboardManager.current
    val error = vm.error

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        ScreenHeader(
            title = type.title,
            subtitle = if (vm.isEditing) "Edited · ${formatDate(vm.createdAt)}" else formatDate(vm.createdAt),
            navigation = { CircleIconButton(Icons.Rounded.Close, "Close", onClose) },
            action = {
                Button(
                    onClick = { if (vm.save(thenWrite = false)) onClose() },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) { Text("Save") }
            },
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Row(
                Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBadge(type.icon, type.color, size = 48.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(type.description, style = MaterialTheme.typography.titleSmall)
                    Text(type.category.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            type.fields.forEach { spec ->
                FieldLabel(spec)
                if (spec.input == FieldInput.CHOICE) {
                    ChoiceField(spec, vm.fields[spec.key] ?: spec.defaultValue) { vm.fields[spec.key] = it }
                } else {
                    InputField(spec, vm.fields[spec.key].orEmpty()) { vm.fields[spec.key] = it }
                }
            }

            AnimatedVisibility(vm.showErrors && error != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.WarningAmber, null, tint = Red, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(error.orEmpty(), color = Red, style = MaterialTheme.typography.labelLarge)
                }
            }

            SectionLabel("Size")
            InfoCard {
                Row(Modifier.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Memory, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    val size = vm.estimatedSize
                    Column(Modifier.weight(1f)) {
                        Text(if (size > 0) formatBytes(size) else "—", style = MaterialTheme.typography.titleSmall)
                        Text(
                            fitHint(size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            SectionLabel("Actions")
            InfoCard {
                ActionRow(Icons.Rounded.ContentPaste, "Paste from clipboard", MaterialTheme.colorScheme.onSurface) {
                    val text = clipboard.getText()?.text.orEmpty()
                    val target = type.fields.firstOrNull { it.input != FieldInput.CHOICE && vm.fields[it.key].isNullOrBlank() }
                        ?: type.fields.first { it.input != FieldInput.CHOICE }
                    if (text.isNotEmpty()) vm.fields[target.key] = text
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ActionRow(Icons.Rounded.DeleteSweep, "Clear all fields", Red) {
                    vm.fields.clear()
                    vm.fields.putAll(type.defaults())
                    vm.showErrors = false
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        PrimaryPillButton(
            "Save & Write to Tag",
            { if (vm.save(thenWrite = true)) onClose() },
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            icon = Icons.Rounded.Nfc,
            containerColor = Orange,
        )
    }
}

@Composable
private fun FieldLabel(spec: FieldSpec) {
    Row(Modifier.padding(start = 4.dp, top = 18.dp, bottom = 8.dp)) {
        Text(spec.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (spec.required) Text("*", style = MaterialTheme.typography.labelSmall, color = Red)
    }
}

@Composable
private fun InputField(spec: FieldSpec, value: String, onChange: (String) -> Unit) {
    var reveal by remember { mutableStateOf(false) }
    val multiline = spec.input == FieldInput.MULTILINE
    TextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(spec.placeholder) },
        singleLine = !multiline,
        minLines = if (multiline) 3 else 1,
        shape = if (multiline) MaterialTheme.shapes.large else CircleShape,
        keyboardOptions = KeyboardOptions(
            capitalization = if (spec.input == FieldInput.TEXT || multiline) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
            autoCorrectEnabled = spec.input == FieldInput.TEXT || multiline,
            keyboardType = when (spec.input) {
                FieldInput.URL -> KeyboardType.Uri
                FieldInput.PHONE -> KeyboardType.Phone
                FieldInput.EMAIL -> KeyboardType.Email
                FieldInput.DECIMAL -> KeyboardType.Decimal
                FieldInput.PASSWORD -> KeyboardType.Password
                else -> KeyboardType.Text
            },
        ),
        visualTransformation = if (spec.input == FieldInput.PASSWORD && !reveal) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (spec.input == FieldInput.PASSWORD) {
            {
                IconButton(onClick = { reveal = !reveal }) {
                    Icon(if (reveal) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, "Toggle password")
                }
            }
        } else null,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ChoiceField(spec: FieldSpec, value: String, onChange: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, CircleShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        spec.options.forEach { option ->
            val selected = option == value
            Box(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                    .clickable { onChange(option) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    option,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = color)
            Spacer(Modifier.width(14.dp))
            Text(label, style = MaterialTheme.typography.titleSmall, color = color)
        }
    }
}

/** Tells the user which common NTAG chips can hold this payload (NDEF TLV adds ~2-4 bytes). */
private fun fitHint(size: Int): String {
    if (size <= 0) return "Fill in the required fields to see the size"
    val chips = listOf("NTAG213" to 137, "NTAG215" to 496, "NTAG216" to 868)
    val fit = chips.firstOrNull { size + 4 <= it.second }
    return fit?.let { "Fits on ${it.first} and larger tags" } ?: "Larger than NTAG216 — needs a high-capacity tag"
}
