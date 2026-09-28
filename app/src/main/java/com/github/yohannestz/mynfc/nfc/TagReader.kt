package com.github.yohannestz.mynfc.nfc

import android.nfc.NdefMessage
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import com.github.yohannestz.mynfc.data.model.InfoEntry
import com.github.yohannestz.mynfc.data.model.ScannedTag
import java.io.Closeable
import java.util.UUID

/** Reads a [Tag] into a [ScannedTag]. Must run off the main thread (does blocking I/O). */
object TagReader {

    fun read(tag: Tag): ScannedTag {
        val uid = tag.id ?: ByteArray(0)
        val techs = tag.techList.map { it.substringAfterLast('.') }
        val chip = mutableListOf<InfoEntry>()
        var tagType: String? = null

        NfcA.get(tag)?.let { nfcA ->
            chip += InfoEntry("ATQA", "0x" + nfcA.atqa.reversedArray().toHex())
            chip += InfoEntry("SAK", nfcA.sak.toInt().hex2())
            chip += InfoEntry("Max transceive", "${nfcA.maxTransceiveLength} bytes")
            if (MifareUltralight.get(tag) != null || nfcA.sak.toInt() == 0x00) {
                getVersion(nfcA)?.let { v ->
                    chip += InfoEntry("Vendor ID", v[1].toInt().hex2())
                    chip += InfoEntry("Product type", v[2].toInt().hex2())
                    chip += InfoEntry("Product subtype", v[3].toInt().hex2())
                    chip += InfoEntry("Major version", v[4].toInt().hex2())
                    chip += InfoEntry("Minor version", v[5].toInt().hex2())
                    chip += InfoEntry("Storage size", v[6].toInt().hex2())
                    chip += InfoEntry("Protocol type", v[7].toInt().hex2())
                    tagType = identifyByVersion(v)
                }
            }
        }

        MifareClassic.get(tag)?.let { mc ->
            chip += InfoEntry("Mifare type", when (mc.type) {
                MifareClassic.TYPE_CLASSIC -> "Classic"
                MifareClassic.TYPE_PLUS -> "Plus"
                MifareClassic.TYPE_PRO -> "Pro"
                else -> "Unknown"
            })
            chip += InfoEntry("Sectors", "${mc.sectorCount}")
            chip += InfoEntry("Blocks", "${mc.blockCount}")
            tagType = tagType ?: "MIFARE Classic ${if (mc.size >= 1024) "${mc.size / 1024}K" else "${mc.size}B"}"
        }

        MifareUltralight.get(tag)?.let { mu ->
            tagType = tagType ?: when (mu.type) {
                MifareUltralight.TYPE_ULTRALIGHT_C -> "MIFARE Ultralight C"
                else -> "MIFARE Ultralight"
            }
        }

        IsoDep.get(tag)?.let { iso ->
            iso.historicalBytes?.takeIf { it.isNotEmpty() }?.let { chip += InfoEntry("Historical bytes", it.toHex(" ")) }
            iso.hiLayerResponse?.takeIf { it.isNotEmpty() }?.let { chip += InfoEntry("Hi-layer response", it.toHex(" ")) }
            chip += InfoEntry("Extended APDU", if (iso.isExtendedLengthApduSupported) "Supported" else "Not supported")
            tagType = tagType ?: "ISO 14443-4"
        }

        NfcB.get(tag)?.let { b ->
            chip += InfoEntry("Application data", b.applicationData.toHex(" "))
            chip += InfoEntry("Protocol info", b.protocolInfo.toHex(" "))
            tagType = tagType ?: "ISO 14443-3B"
        }

        NfcF.get(tag)?.let { f ->
            chip += InfoEntry("Manufacturer bytes", f.manufacturer.toHex(" "))
            chip += InfoEntry("System code", f.systemCode.toHex(" "))
            tagType = tagType ?: "FeliCa"
        }

        NfcV.get(tag)?.let { v ->
            chip += InfoEntry("DSFID", v.dsfId.toInt().hex2())
            chip += InfoEntry("Response flags", v.responseFlags.toInt().hex2())
            tagType = tagType ?: "ISO 15693"
        }

        var ndefType: String? = null
        var maxSize: Int? = null
        var writable: Boolean? = null
        var canLock: Boolean? = null
        var message: NdefMessage? = null
        Ndef.get(tag)?.let { ndef ->
            ndefType = ndefTypeName(ndef.type)
            maxSize = ndef.maxSize
            writable = ndef.isWritable
            canLock = ndef.canMakeReadOnly()
            message = ndef.cachedNdefMessage
                ?: runCatching { ndef.use { it.connect(); it.ndefMessage } }.getOrNull()
        }

        val manufacturer = Manufacturers.fromUid(uid)
        return ScannedTag(
            id = UUID.randomUUID().toString(),
            scannedAt = System.currentTimeMillis(),
            uid = uid.toHex(":"),
            tagType = tagType ?: if (NfcA.get(tag) != null) "ISO 14443-3A" else "NFC Tag",
            technologies = techs,
            manufacturer = manufacturer,
            ndefType = ndefType,
            maxSize = maxSize,
            usedSize = message?.byteArrayLength,
            writable = writable,
            canMakeReadOnly = canLock,
            formatable = NdefFormatable.get(tag) != null,
            records = NdefParser.parse(message),
            chipInfo = chip,
        )
    }

    /** NXP GET_VERSION (0x60) for NTAG / Ultralight EV1. */
    private fun getVersion(nfcA: NfcA): ByteArray? = runCatching {
        nfcA.use {
            it.connect()
            it.transceive(byteArrayOf(0x60)).takeIf { r -> r.size >= 8 }
        }
    }.getOrNull()

    private fun identifyByVersion(v: ByteArray): String? {
        val vendor = v[1].toInt()
        val product = v[2].toInt()
        val storage = v[6].toInt() and 0xFF
        if (vendor != 0x04) return null
        return when (product) {
            0x04 -> when (storage) {
                0x0F -> "NTAG213"
                0x11 -> "NTAG215"
                0x13 -> "NTAG216"
                0x0B -> "NTAG210"
                0x0E -> "NTAG212"
                else -> "NTAG"
            }
            0x03 -> when (storage) {
                0x0B -> "MIFARE Ultralight EV1 (MF0UL11)"
                0x0E -> "MIFARE Ultralight EV1 (MF0UL21)"
                else -> "MIFARE Ultralight EV1"
            }
            else -> null
        }
    }

    private fun ndefTypeName(type: String?): String = when (type) {
        Ndef.NFC_FORUM_TYPE_1 -> "NFC Forum Type 1"
        Ndef.NFC_FORUM_TYPE_2 -> "NFC Forum Type 2"
        Ndef.NFC_FORUM_TYPE_3 -> "NFC Forum Type 3"
        Ndef.NFC_FORUM_TYPE_4 -> "NFC Forum Type 4"
        Ndef.MIFARE_CLASSIC -> "MIFARE Classic"
        else -> type?.substringAfterLast('.') ?: "Unknown"
    }

    private inline fun <T : Closeable, R> T.use(block: (T) -> R): R = try {
        block(this)
    } finally {
        runCatching { close() }
    }
}
