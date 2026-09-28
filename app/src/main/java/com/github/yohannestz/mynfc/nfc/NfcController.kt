package com.github.yohannestz.mynfc.nfc

import android.content.Context
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import com.github.yohannestz.mynfc.data.TagRepository
import com.github.yohannestz.mynfc.data.model.ScannedTag
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NfcStatus { UNSUPPORTED, DISABLED, READY }

sealed interface NfcOperation {
    val title: String

    data class Write(val message: NdefMessage, val lock: Boolean = false, override val title: String = "Write to tag") : NfcOperation
    data object Erase : NfcOperation { override val title = "Erase tag" }
    data object Lock : NfcOperation { override val title = "Lock tag" }
    data object Format : NfcOperation { override val title = "Format tag" }
    data object Clone : NfcOperation { override val title = "Copy tag" }
}

sealed interface SessionState {
    data object Idle : SessionState
    data class Waiting(val operation: NfcOperation, val hint: String, val step: Int = 0) : SessionState
    data class Working(val operation: NfcOperation) : SessionState
    data class Success(val operation: NfcOperation, val message: String) : SessionState
    data class Failed(val operation: NfcOperation, val message: String) : SessionState
}

/**
 * Single point that receives every tag the phone sees and routes it:
 * with no active operation tags are read and published on [scannedTags];
 * otherwise the pending write/erase/lock/format/clone operation runs.
 */
class NfcController(private val repository: TagRepository) {

    private val _session = MutableStateFlow<SessionState>(SessionState.Idle)
    val session: StateFlow<SessionState> = _session.asStateFlow()

    private val _scannedTags = MutableSharedFlow<ScannedTag>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val scannedTags: SharedFlow<ScannedTag> = _scannedTags.asSharedFlow()

    private val _readErrors = MutableSharedFlow<String>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val readErrors: SharedFlow<String> = _readErrors.asSharedFlow()

    @Volatile private var cloneSource: Pair<String, NdefMessage>? = null

    fun status(context: Context): NfcStatus {
        val adapter = NfcAdapter.getDefaultAdapter(context) ?: return NfcStatus.UNSUPPORTED
        return if (adapter.isEnabled) NfcStatus.READY else NfcStatus.DISABLED
    }

    fun start(operation: NfcOperation) {
        cloneSource = null
        _session.value = SessionState.Waiting(operation, hintFor(operation))
    }

    fun dismiss() {
        cloneSource = null
        _session.value = SessionState.Idle
    }

    /** Called on a binder thread by reader mode. */
    fun onTagDiscovered(tag: Tag) {
        when (val state = _session.value) {
            SessionState.Idle -> read(tag)
            is SessionState.Waiting -> perform(state, tag)
            is SessionState.Failed -> perform(SessionState.Waiting(state.operation, hintFor(state.operation)), tag)
            is SessionState.Working, is SessionState.Success -> Unit
        }
    }

    private fun read(tag: Tag) {
        runCatching { TagReader.read(tag) }
            .onSuccess {
                repository.remember(it)
                _scannedTags.tryEmit(it)
            }
            .onFailure { _readErrors.tryEmit("Couldn't read tag. Hold it still and try again.") }
    }

    private fun perform(state: SessionState.Waiting, tag: Tag) {
        val op = state.operation
        _session.value = SessionState.Working(op)
        try {
            val message = when (op) {
                is NfcOperation.Write -> {
                    TagWriter.write(tag, op.message, op.lock)
                    if (op.lock) "Written and locked" else "Written successfully"
                }
                NfcOperation.Erase -> { TagWriter.erase(tag); "Tag erased" }
                NfcOperation.Lock -> { TagWriter.lock(tag); "Tag is now read-only" }
                NfcOperation.Format -> { TagWriter.format(tag); "Tag formatted" }
                NfcOperation.Clone -> clone(tag) ?: return
            }
            _session.value = SessionState.Success(op, message)
        } catch (e: NfcWriteException) {
            _session.value = SessionState.Failed(op, e.message ?: "Operation failed")
        } catch (e: Exception) {
            _session.value = SessionState.Failed(op, "Unexpected error: ${e.message}")
        }
    }

    /** Returns a success message, or null when we're now waiting for the target tag. */
    private fun clone(tag: Tag): String? {
        val uid = tag.id?.toHex().orEmpty()
        val source = cloneSource
        if (source == null) {
            val scanned = TagReader.read(tag)
            if (!scanned.hasNdefData) throw NfcWriteException("Source tag has no NDEF data to copy")
            val message = NdefMessage(scanned.records.map(NdefParser::toRecord).toTypedArray())
            cloneSource = uid to message
            _session.value = SessionState.Waiting(NfcOperation.Clone, "Copied ${scanned.records.size} record(s). Now tap the target tag.", step = 1)
            return null
        }
        if (source.first == uid) {
            _session.value = SessionState.Waiting(NfcOperation.Clone, "That's the source tag. Tap a different tag.", step = 1)
            return null
        }
        TagWriter.write(tag, source.second)
        cloneSource = null
        return "Tag copied"
    }

    private fun hintFor(op: NfcOperation) = when (op) {
        is NfcOperation.Write -> "Hold a tag near the back of your phone to write"
        NfcOperation.Erase -> "Hold the tag you want to erase"
        NfcOperation.Lock -> "Hold the tag you want to lock permanently"
        NfcOperation.Format -> "Hold the tag you want to format"
        NfcOperation.Clone -> "Tap the source tag to copy its data"
    }
}
