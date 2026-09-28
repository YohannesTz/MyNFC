package com.github.yohannestz.mynfc.nfc

import android.nfc.FormatException
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import java.io.IOException

class NfcWriteException(message: String) : Exception(message)

/** Blocking tag write operations. Call from the reader-mode callback thread. */
object TagWriter {

    private val EMPTY_MESSAGE = NdefMessage(NdefRecord(NdefRecord.TNF_EMPTY, null, null, null))

    fun write(tag: Tag, message: NdefMessage, makeReadOnly: Boolean = false) = guarded {
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            ndef.connect()
            try {
                if (!ndef.isWritable) throw NfcWriteException("This tag is read-only")
                val size = message.byteArrayLength
                if (size > ndef.maxSize) {
                    throw NfcWriteException("Data is $size bytes but the tag only holds ${ndef.maxSize} bytes")
                }
                ndef.writeNdefMessage(message)
                if (makeReadOnly && !ndef.makeReadOnly()) throw NfcWriteException("Data written, but the tag could not be locked")
            } finally {
                runCatching { ndef.close() }
            }
            return@guarded
        }
        val formatable = NdefFormatable.get(tag) ?: throw NfcWriteException("This tag doesn't support NDEF")
        formatable.connect()
        try {
            if (makeReadOnly) formatable.formatReadOnly(message) else formatable.format(message)
        } finally {
            runCatching { formatable.close() }
        }
    }

    fun erase(tag: Tag) = write(tag, EMPTY_MESSAGE)

    fun lock(tag: Tag) = guarded {
        val ndef = Ndef.get(tag) ?: throw NfcWriteException("This tag doesn't support NDEF")
        ndef.connect()
        try {
            if (!ndef.canMakeReadOnly()) throw NfcWriteException("This tag can't be made read-only")
            if (!ndef.makeReadOnly()) throw NfcWriteException("Locking failed")
        } finally {
            runCatching { ndef.close() }
        }
    }

    fun format(tag: Tag) = guarded {
        if (Ndef.get(tag) != null) {
            write(tag, EMPTY_MESSAGE)
            return@guarded
        }
        val formatable = NdefFormatable.get(tag) ?: throw NfcWriteException("This tag can't be formatted as NDEF")
        formatable.connect()
        try {
            formatable.format(EMPTY_MESSAGE)
        } finally {
            runCatching { formatable.close() }
        }
    }

    private inline fun guarded(block: () -> Unit) {
        try {
            block()
        } catch (e: NfcWriteException) {
            throw e
        } catch (e: TagLostException) {
            throw NfcWriteException("Tag moved away too soon. Hold it still and try again.")
        } catch (e: FormatException) {
            throw NfcWriteException("Invalid NDEF data: ${e.message}")
        } catch (e: IOException) {
            throw NfcWriteException("Communication error. Hold the tag still and try again.")
        } catch (e: SecurityException) {
            throw NfcWriteException("Tag is out of date. Tap it again.")
        }
    }
}
