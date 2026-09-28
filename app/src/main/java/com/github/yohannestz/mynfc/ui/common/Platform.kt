package com.github.yohannestz.mynfc.ui.common

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.github.yohannestz.mynfc.nfc.NfcStatus
import com.github.yohannestz.mynfc.AppContainer
import com.github.yohannestz.mynfc.data.model.NdefRecordInfo
import com.github.yohannestz.mynfc.data.model.RecordKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }
val LocalSnackbar = staticCompositionLocalOf<SnackbarHostState> { error("SnackbarHostState not provided") }

fun Context.copyToClipboard(label: String, text: String) {
    val clipboard = getSystemService(ClipboardManager::class.java)
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

fun Context.shareText(subject: String, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    startActivity(Intent.createChooser(send, subject))
}

fun Context.openNfcSettings() {
    runCatching { startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }
        .onFailure { startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS)) }
}

/** Whether a record has a sensible system action (open link, dial, etc.). */
val NdefRecordInfo.isActionable: Boolean
    get() = kind in setOf(RecordKind.URL, RecordKind.PHONE, RecordKind.EMAIL, RecordKind.SMS, RecordKind.GEO, RecordKind.APP) ||
        (kind == RecordKind.SMART_POSTER && value.lines().any { it.contains(':') })

fun Context.openRecord(record: NdefRecordInfo) {
    val intent = when (record.kind) {
        RecordKind.APP -> packageManager.getLaunchIntentForPackage(record.value)
            ?: Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${record.value}"))
        RecordKind.SMART_POSTER -> Intent(Intent.ACTION_VIEW, Uri.parse(record.value.lines().last { it.contains(':') }))
        else -> Intent(Intent.ACTION_VIEW, Uri.parse(record.value))
    }
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, "No app can open this", Toast.LENGTH_SHORT).show()
    }
}

/** Lets the user pick where to save a JSON export using the system file picker. */
class JsonExporter internal constructor(private val launch: (String, () -> String) -> Unit) {
    fun export(fileName: String, content: () -> String) = launch(fileName, content)
}

@Composable
fun rememberJsonExporter(onResult: (Boolean) -> Unit): JsonExporter {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<(() -> String)?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val content = pending
        pending = null
        if (uri == null || content == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(content().toByteArray()) }
                }.isSuccess
            }
            onResult(ok)
        }
    }
    return remember(launcher) {
        JsonExporter { name, content ->
            pending = content
            launcher.launch(name)
        }
    }
}

@Composable
fun rememberJsonImporter(onText: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() } }.getOrNull()
            }
            if (text != null) onText(text)
        }
    }
    return remember(launcher) { { launcher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) } }
}

/** NFC availability, refreshed every time the screen resumes (e.g. returning from Settings). */
@Composable
fun rememberNfcStatus(): NfcStatus {
    val context = LocalContext.current
    val controller = LocalAppContainer.current.nfcController
    var status by remember { mutableStateOf(controller.status(context)) }
    LifecycleResumeEffect(controller) {
        status = controller.status(context)
        onPauseOrDispose { }
    }
    return status
}
