package com.github.yohannestz.mynfc.nfc

import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.NfcA
import android.nfc.tech.NfcV
import com.github.yohannestz.mynfc.data.model.RfidFamily
import java.io.IOException

/** Writes a single memory unit back to a tag. Blocking; runs off the main thread. */
object RawTagWriter {

    /** Bytes expected for one write on each family. */
    fun unitSize(family: RfidFamily): Int = when (family) {
        RfidFamily.MIFARE_CLASSIC -> MifareClassic.BLOCK_SIZE
        RfidFamily.ULTRALIGHT -> MifareUltralight.PAGE_SIZE
        RfidFamily.ISO15693 -> 4
        RfidFamily.UNKNOWN -> 0
    }

    /**
     * @param index absolute block (Classic), page (Ultralight) or block (ISO 15693) number.
     * @param data exactly [unitSize] bytes.
     */
    fun writeUnit(tag: Tag, family: RfidFamily, index: Int, data: ByteArray) = guarded {
        when (family) {
            RfidFamily.MIFARE_CLASSIC -> writeClassic(tag, index, data)
            RfidFamily.ULTRALIGHT -> writeUltralight(tag, index, data)
            RfidFamily.ISO15693 -> writeIso15693(tag, index, data)
            RfidFamily.UNKNOWN -> throw NfcWriteException("This tag doesn't support raw writing")
        }
    }

    private fun writeClassic(tag: Tag, block: Int, data: ByteArray) {
        require(data.size == MifareClassic.BLOCK_SIZE) { "Block must be 16 bytes" }
        val mc = MifareClassic.get(tag) ?: throw NfcWriteException("Not a MIFARE Classic tag")
        val sector = mc.blockToSector(block)
        if (block == 0) throw NfcWriteException("Block 0 holds the read-only UID and can't be written")
        mc.connect()
        try {
            val ok = DEFAULT_KEYS.any { runCatching { mc.authenticateSectorWithKeyA(sector, it) }.getOrDefault(false) } ||
                DEFAULT_KEYS.any { runCatching { mc.authenticateSectorWithKeyB(sector, it) }.getOrDefault(false) }
            if (!ok) throw NfcWriteException("Sector $sector uses a custom key — can't authenticate")
            mc.writeBlock(block, data)
        } finally {
            runCatching { mc.close() }
        }
    }

    private fun writeUltralight(tag: Tag, page: Int, data: ByteArray) {
        require(data.size == MifareUltralight.PAGE_SIZE) { "Page must be 4 bytes" }
        if (page < 4) throw NfcWriteException("Pages 0–3 are UID and lock bits — writing them can brick the tag")
        val mu = MifareUltralight.get(tag)
        if (mu != null) {
            mu.connect()
            try {
                mu.writePage(page, data)
            } finally {
                runCatching { mu.close() }
            }
            return
        }
        val nfcA = NfcA.get(tag) ?: throw NfcWriteException("Not an Ultralight/NTAG tag")
        nfcA.connect()
        try {
            nfcA.transceive(byteArrayOf(0xA2.toByte(), page.toByte()) + data)
        } finally {
            runCatching { nfcA.close() }
        }
    }

    private fun writeIso15693(tag: Tag, block: Int, data: ByteArray) {
        val nfcV = NfcV.get(tag) ?: throw NfcWriteException("Not an ISO 15693 tag")
        val uid = tag.id ?: throw NfcWriteException("Missing UID")
        nfcV.connect()
        try {
            val cmd = byteArrayOf(0x22, 0x21) + uid + block.toByte() + data
            val resp = nfcV.transceive(cmd)
            if (resp.isEmpty() || resp[0].toInt() != 0x00) throw NfcWriteException("Tag rejected the write (block $block)")
        } finally {
            runCatching { nfcV.close() }
        }
    }

    private val DEFAULT_KEYS: List<ByteArray> = listOf(
        MifareClassic.KEY_DEFAULT,
        MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY,
        MifareClassic.KEY_NFC_FORUM,
        "A0A1A2A3A4A5".hexToBytes(),
        "D3F7D3F7D3F7".hexToBytes(),
    ).distinctBy { it.toHex() }

    private inline fun guarded(block: () -> Unit) {
        try {
            block()
        } catch (e: NfcWriteException) {
            throw e
        } catch (e: TagLostException) {
            throw NfcWriteException("Tag moved away too soon. Hold it still and try again.")
        } catch (e: IOException) {
            throw NfcWriteException("Communication error. Hold the tag still and try again.")
        } catch (e: IllegalArgumentException) {
            throw NfcWriteException(e.message ?: "Invalid data")
        }
    }
}
