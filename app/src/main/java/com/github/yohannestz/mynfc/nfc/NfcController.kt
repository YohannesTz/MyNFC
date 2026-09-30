package com.github.yohannestz.mynfc.nfc

import android.content.Context
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import com.github.yohannestz.mynfc.data.TagRepository
import com.github.yohannestz.mynfc.data.model.MemoryBlock
import com.github.yohannestz.mynfc.data.model.RfidDump
import com.github.yohannestz.mynfc.data.model.RfidFamily
import com.github.yohannestz.mynfc.data.model.ScannedTag
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NfcStatus { UNSUPPORTED, DISABLED, READY }

/** What idle taps do: decode NDEF (default), or dump raw memory (RFID tab). */
enum class ScanMode { NDEF, RAW }

/** Raw-memory write operations, mirroring [NfcOperation] for the RFID side. */
sealed interface RawOperation {
    val title: String

    /** Write one 16/4-byte unit at [index] on a [family] tag. */
    data class WriteUnit(val family: RfidFamily, val index: Int, val data: ByteArray, override val title: String = "Write block") : RawOperation {
        override fun equals(other: Any?) = other is WriteUnit && index == other.index && family == other.family && data.contentEquals(other.data)
        override fun hashCode() = 31 * (31 * index + family.hashCode()) + data.contentHashCode()
    }

    /** Write every writable block from a saved dump onto a matching tag. */
    data class Restore(val blocks: List<MemoryBlock>, val family: RfidFamily, override val title: String = "Restore dump") : RawOperation
}

sealed interface RawSessionState {
    data object Idle : RawSessionState
    data class Waiting(val operation: RawOperation, val hint: String) : RawSessionState
    data class Working(val operation: RawOperation, val progress: Float = 0f) : RawSessionState
    data class Success(val operation: RawOperation, val message: String) : RawSessionState
    data class Failed(val operation: RawOperation, val message: String) : RawSessionState
}

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

    // --- Raw (RFID) side ---
    @Volatile var scanMode: ScanMode = ScanMode.NDEF

    private val _rawSession = MutableStateFlow<RawSessionState>(RawSessionState.Idle)
    val rawSession: StateFlow<RawSessionState> = _rawSession.asStateFlow()

    private val _scannedDumps = MutableSharedFlow<RfidDump>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val scannedDumps: SharedFlow<RfidDump> = _scannedDumps.asSharedFlow()

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

    fun startRaw(operation: RawOperation) {
        _rawSession.value = RawSessionState.Waiting(operation, rawHintFor(operation))
    }

    fun dismissRaw() {
        _rawSession.value = RawSessionState.Idle
    }

    /** Called on a binder thread by reader mode. */
    fun onTagDiscovered(tag: Tag) {
        // A pending raw write takes priority over everything.
        when (val raw = _rawSession.value) {
            is RawSessionState.Waiting -> { performRaw(raw.operation, tag); return }
            is RawSessionState.Failed -> { performRaw(raw.operation, tag); return }
            else -> Unit
        }
        when (val state = _session.value) {
            SessionState.Idle -> if (scanMode == ScanMode.RAW) readRaw(tag) else read(tag)
            is SessionState.Waiting -> perform(state, tag)
            is SessionState.Failed -> perform(SessionState.Waiting(state.operation, hintFor(state.operation)), tag)
            is SessionState.Working, is SessionState.Success -> Unit
        }
    }

    private fun readRaw(tag: Tag) {
        runCatching { RawTagReader.read(tag) }
            .onSuccess {
                repository.rememberDump(it)
                _scannedDumps.tryEmit(it)
            }
            .onFailure { _readErrors.tryEmit("Couldn't read tag memory. Hold it still and try again.") }
    }

    private fun performRaw(op: RawOperation, tag: Tag) {
        _rawSession.value = RawSessionState.Working(op)
        try {
            val message = when (op) {
                is RawOperation.WriteUnit -> {
                    RawTagWriter.writeUnit(tag, op.family, op.index, op.data)
                    "Block ${op.index} written"
                }
                is RawOperation.Restore -> restore(op, tag)
            }
            _rawSession.value = RawSessionState.Success(op, message)
        } catch (e: NfcWriteException) {
            _rawSession.value = RawSessionState.Failed(op, e.message ?: "Write failed")
        } catch (e: Exception) {
            _rawSession.value = RawSessionState.Failed(op, "Unexpected error: ${e.message}")
        }
    }

    private fun restore(op: RawOperation.Restore, tag: Tag): String {
        val targetFamily = RawTagReader.family(tag)
        if (targetFamily != op.family) throw NfcWriteException("Target is ${targetFamily.label}, but the dump is ${op.family.label}")
        val writable = op.blocks.filter { it.writable && it.readable }
        if (writable.isEmpty()) throw NfcWriteException("This dump has no writable blocks")
        var done = 0
        writable.forEach { block ->
            RawTagWriter.writeUnit(tag, op.family, block.index, block.dataHex.hexToBytes())
            done++
            _rawSession.value = RawSessionState.Working(op, done.toFloat() / writable.size)
        }
        return "Restored $done block(s)"
    }

    private fun rawHintFor(op: RawOperation) = when (op) {
        is RawOperation.WriteUnit -> "Hold the same tag to write block ${op.index}"
        is RawOperation.Restore -> "Hold a blank ${op.family.label} tag to restore onto"
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
