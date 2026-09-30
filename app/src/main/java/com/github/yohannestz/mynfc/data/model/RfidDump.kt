package com.github.yohannestz.mynfc.data.model

import kotlinx.serialization.Serializable

/** Family of HF (13.56 MHz) chip whose raw memory we can access with the phone. */
@Serializable
enum class RfidFamily(val label: String, val unit: String) {
    MIFARE_CLASSIC("MIFARE Classic", "block"),
    ULTRALIGHT("Ultralight / NTAG", "page"),
    ISO15693("ISO 15693", "block"),
    UNKNOWN("Unknown", "block"),
}

/** One addressable memory unit (a 16-byte Classic block, a 4-byte NTAG page, an ISO 15693 block). */
@Serializable
data class MemoryBlock(
    val index: Int,
    /** Sector number for MIFARE Classic; null for flat page/block layouts. */
    val sector: Int? = null,
    val dataHex: String,
    val readable: Boolean = true,
    val writable: Boolean = true,
    /** Role hint for coloring: "data", "sector-trailer", "manufacturer", "config", "counter", "lock". */
    val role: String = "data",
    /** Key that unlocked this block's sector (Classic only), for information. */
    val note: String? = null,
) {
    val sizeBytes: Int get() = dataHex.length / 2
}

/** A full raw-memory snapshot of a tag. */
@Serializable
data class RfidDump(
    val id: String,
    val scannedAt: Long,
    val name: String? = null,
    val family: RfidFamily,
    val uid: String,
    val tagType: String,
    val technologies: List<String> = emptyList(),
    val manufacturer: String? = null,
    val totalBytes: Int,
    val blockSize: Int,
    val blocks: List<MemoryBlock> = emptyList(),
    /** Which sectors authenticated, out of how many (Classic). */
    val sectorsRead: Int = 0,
    val sectorsTotal: Int = 0,
    /** Distinct keys that worked, hex, for the user's reference. */
    val keysUsed: List<String> = emptyList(),
    val info: List<InfoEntry> = emptyList(),
) {
    val displayName: String get() = name?.takeIf { it.isNotBlank() } ?: tagType
    val readableBlocks: Int get() = blocks.count { it.readable }
    val partial: Boolean get() = sectorsTotal > 0 && sectorsRead < sectorsTotal
}

@Serializable
data class RfidBundle(
    val app: String = "MyNFC",
    val formatVersion: Int = 1,
    val exportedAt: Long,
    val dumps: List<RfidDump> = emptyList(),
)
