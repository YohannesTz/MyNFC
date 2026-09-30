package com.github.yohannestz.mynfc.nfc

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.NfcA
import android.nfc.tech.NfcV
import com.github.yohannestz.mynfc.data.model.InfoEntry
import com.github.yohannestz.mynfc.data.model.MemoryBlock
import com.github.yohannestz.mynfc.data.model.RfidDump
import com.github.yohannestz.mynfc.data.model.RfidFamily
import java.util.UUID

/**
 * Reads the raw memory of an HF tag into an [RfidDump]. Blocking; runs off the main thread.
 *
 * For MIFARE Classic each sector is protected by a key; this tries the well-known
 * factory/transport keys that ship blank from vendors (published in the NXP datasheet and
 * the Android [MifareClassic] constants). Sectors that use custom keys are reported as
 * unreadable rather than skipped, so the dump makes clear what was and wasn't accessible.
 */
object RawTagReader {

    /** Factory-default keys documented for blank tags. */
    private val DEFAULT_KEYS: List<ByteArray> = listOf(
        MifareClassic.KEY_DEFAULT,
        MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY,
        MifareClassic.KEY_NFC_FORUM,
        "A0A1A2A3A4A5".hexToBytes(),
        "D3F7D3F7D3F7".hexToBytes(),
        "000000000000".hexToBytes(),
        "FFFFFFFFFFFF".hexToBytes(),
    ).distinctBy { it.toHex() }

    fun family(tag: Tag): RfidFamily = when {
        MifareClassic.get(tag) != null -> RfidFamily.MIFARE_CLASSIC
        MifareUltralight.get(tag) != null -> RfidFamily.ULTRALIGHT
        NfcV.get(tag) != null -> RfidFamily.ISO15693
        NfcA.get(tag) != null -> RfidFamily.ULTRALIGHT
        else -> RfidFamily.UNKNOWN
    }

    fun read(tag: Tag): RfidDump = when (family(tag)) {
        RfidFamily.MIFARE_CLASSIC -> readClassic(tag, MifareClassic.get(tag))
        RfidFamily.ULTRALIGHT -> readUltralight(tag)
        RfidFamily.ISO15693 -> readIso15693(tag, NfcV.get(tag))
        RfidFamily.UNKNOWN -> RfidDump(
            id = UUID.randomUUID().toString(),
            scannedAt = System.currentTimeMillis(),
            family = RfidFamily.UNKNOWN,
            uid = tag.id?.toHex(":").orEmpty(),
            tagType = "Unsupported for raw access",
            technologies = tag.techList.map { it.substringAfterLast('.') },
            totalBytes = 0,
            blockSize = 0,
        )
    }

    private fun readClassic(tag: Tag, mc: MifareClassic): RfidDump = mc.use {
        mc.connect()
        val blocks = mutableListOf<MemoryBlock>()
        val keysUsed = linkedSetOf<String>()
        var sectorsRead = 0
        for (sector in 0 until mc.sectorCount) {
            val key = authenticate(mc, sector)
            val first = mc.sectorToBlock(sector)
            val count = mc.getBlockCountInSector(sector)
            if (key != null) {
                sectorsRead++
                keysUsed += key.toHex()
                for (b in 0 until count) {
                    val absolute = first + b
                    val isTrailer = b == count - 1
                    val data = runCatching { mc.readBlock(absolute) }.getOrNull()
                    blocks += MemoryBlock(
                        index = absolute,
                        sector = sector,
                        dataHex = data?.toHex() ?: "".padEnd(32, '0'),
                        readable = data != null,
                        writable = data != null && absolute != 0,
                        role = when {
                            absolute == 0 -> "manufacturer"
                            isTrailer -> "sector-trailer"
                            else -> "data"
                        },
                        note = if (isTrailer) "Key ${key.toHex()}" else null,
                    )
                }
            } else {
                for (b in 0 until count) {
                    val absolute = first + b
                    blocks += MemoryBlock(
                        index = absolute,
                        sector = sector,
                        dataHex = "".padEnd(32, '0'),
                        readable = false,
                        writable = false,
                        role = if (b == count - 1) "sector-trailer" else "data",
                        note = "Custom key",
                    )
                }
            }
        }
        val typeName = when (mc.type) {
            MifareClassic.TYPE_CLASSIC -> "MIFARE Classic"
            MifareClassic.TYPE_PLUS -> "MIFARE Plus"
            MifareClassic.TYPE_PRO -> "MIFARE Pro"
            else -> "MIFARE Classic"
        }
        RfidDump(
            id = UUID.randomUUID().toString(),
            scannedAt = System.currentTimeMillis(),
            family = RfidFamily.MIFARE_CLASSIC,
            uid = tag.id?.toHex(":").orEmpty(),
            tagType = "$typeName ${if (mc.size >= 1024) "${mc.size / 1024}K" else "${mc.size}B"}",
            technologies = tag.techList.map { it.substringAfterLast('.') },
            manufacturer = tag.id?.let(Manufacturers::fromUid),
            totalBytes = mc.size,
            blockSize = MifareClassic.BLOCK_SIZE,
            blocks = blocks,
            sectorsRead = sectorsRead,
            sectorsTotal = mc.sectorCount,
            keysUsed = keysUsed.toList(),
            info = listOf(
                InfoEntry("Sectors", "${mc.sectorCount}"),
                InfoEntry("Blocks", "${mc.blockCount}"),
                InfoEntry("Block size", "${MifareClassic.BLOCK_SIZE} bytes"),
                InfoEntry("Sectors unlocked", "$sectorsRead / ${mc.sectorCount}"),
            ),
        )
    }

    private fun authenticate(mc: MifareClassic, sector: Int): ByteArray? {
        for (key in DEFAULT_KEYS) {
            if (runCatching { mc.authenticateSectorWithKeyA(sector, key) }.getOrDefault(false)) return key
            if (runCatching { mc.authenticateSectorWithKeyB(sector, key) }.getOrDefault(false)) return key
        }
        return null
    }

    private fun readUltralight(tag: Tag): RfidDump {
        val mu = MifareUltralight.get(tag)
        val nfcA = NfcA.get(tag)
        val pageSize = MifareUltralight.PAGE_SIZE // 4
        val blocks = mutableListOf<MemoryBlock>()
        val pageCount = pageCountFor(tag, nfcA)

        // Read 4 pages per command (16 bytes) and split into pages.
        val reader: (Int) -> ByteArray? = when {
            mu != null -> { p -> runCatching { mu.readPages(p) }.getOrNull() }
            nfcA != null -> { p -> runCatching { nfcA.transceive(byteArrayOf(0x30, p.toByte())) }.getOrNull()?.takeIf { it.size >= 16 } }
            else -> { _ -> null }
        }
        val connectTech = mu ?: nfcA
        connectTech?.let { tech ->
            (tech as? MifareUltralight)?.connect() ?: (tech as? NfcA)?.connect()
        }
        try {
            var page = 0
            while (page < pageCount) {
                val chunk = reader(page)
                if (chunk == null) {
                    blocks += MemoryBlock(page, null, "00000000", readable = false, writable = false, role = roleForPage(page, pageCount))
                    page++
                    continue
                }
                for (i in 0 until 4) {
                    val p = page + i
                    if (p >= pageCount) break
                    blocks += MemoryBlock(
                        index = p,
                        dataHex = chunk.copyOfRange(i * 4, i * 4 + 4).toHex(),
                        writable = p >= 4,
                        role = roleForPage(p, pageCount),
                    )
                }
                page += 4
            }
        } finally {
            runCatching { (connectTech as? MifareUltralight)?.close() ?: (connectTech as? NfcA)?.close() }
        }

        val typeName = detectUltralightType(tag, nfcA, pageCount)
        return RfidDump(
            id = UUID.randomUUID().toString(),
            scannedAt = System.currentTimeMillis(),
            family = RfidFamily.ULTRALIGHT,
            uid = tag.id?.toHex(":").orEmpty(),
            tagType = typeName,
            technologies = tag.techList.map { it.substringAfterLast('.') },
            manufacturer = tag.id?.let(Manufacturers::fromUid),
            totalBytes = pageCount * pageSize,
            blockSize = pageSize,
            blocks = blocks,
            info = listOf(
                InfoEntry("Pages", "$pageCount"),
                InfoEntry("Page size", "$pageSize bytes"),
                InfoEntry("User memory", "${(pageCount - 8).coerceAtLeast(0) * pageSize} bytes"),
            ),
        )
    }

    private fun roleForPage(page: Int, pageCount: Int): String = when {
        page < 2 -> "manufacturer"
        page == 2 -> "lock"
        page == 3 -> "config"
        page >= pageCount - 4 -> "config"
        else -> "data"
    }

    private fun pageCountFor(tag: Tag, nfcA: NfcA?): Int {
        MifareUltralight.get(tag)?.let {
            return when (it.type) {
                MifareUltralight.TYPE_ULTRALIGHT_C -> 48
                else -> 16
            }
        }
        // NTAG via GET_VERSION storage byte.
        val version = nfcA?.let { getVersion(it) }
        return when (version?.getOrNull(6)?.toInt()?.and(0xFF)) {
            0x0F -> 45   // NTAG213
            0x11 -> 135  // NTAG215
            0x13 -> 231  // NTAG216
            else -> 16
        }
    }

    private fun detectUltralightType(tag: Tag, nfcA: NfcA?, pageCount: Int): String {
        MifareUltralight.get(tag)?.let {
            if (it.type == MifareUltralight.TYPE_ULTRALIGHT_C) return "MIFARE Ultralight C"
        }
        return when (pageCount) {
            45 -> "NTAG213"
            135 -> "NTAG215"
            231 -> "NTAG216"
            else -> "MIFARE Ultralight"
        }
    }

    private fun getVersion(nfcA: NfcA): ByteArray? = runCatching {
        val wasConnected = nfcA.isConnected
        if (!wasConnected) nfcA.connect()
        try {
            nfcA.transceive(byteArrayOf(0x60)).takeIf { it.size >= 8 }
        } finally {
            if (!wasConnected) runCatching { nfcA.close() }
        }
    }.getOrNull()

    private fun readIso15693(tag: Tag, nfcV: NfcV): RfidDump = nfcV.use {
        nfcV.connect()
        val uid = tag.id ?: ByteArray(0)
        val blocks = mutableListOf<MemoryBlock>()
        var index = 0
        // Read single block (0x20) until the tag stops responding or we hit 256 blocks.
        while (index < 256) {
            val cmd = byteArrayOf(0x22, 0x20.toByte()) + uid + index.toByte()
            val resp = runCatching { nfcV.transceive(cmd) }.getOrNull()
            if (resp == null || resp.isEmpty() || resp[0].toInt() != 0x00) break
            blocks += MemoryBlock(index, null, resp.copyOfRange(1, resp.size).toHex(), role = "data")
            index++
        }
        val blockSize = blocks.firstOrNull()?.sizeBytes ?: 4
        RfidDump(
            id = UUID.randomUUID().toString(),
            scannedAt = System.currentTimeMillis(),
            family = RfidFamily.ISO15693,
            uid = uid.toHex(":"),
            tagType = "ISO 15693 (${blocks.size} blocks)",
            technologies = tag.techList.map { it.substringAfterLast('.') },
            totalBytes = blocks.size * blockSize,
            blockSize = blockSize,
            blocks = blocks,
            info = listOf(
                InfoEntry("Blocks", "${blocks.size}"),
                InfoEntry("Block size", "$blockSize bytes"),
                InfoEntry("DSFID", nfcV.dsfId.toInt().hex2()),
            ),
        )
    }

    private inline fun <T : java.io.Closeable, R> T.use(block: (T) -> R): R = try {
        block(this)
    } finally {
        runCatching { close() }
    }
}
