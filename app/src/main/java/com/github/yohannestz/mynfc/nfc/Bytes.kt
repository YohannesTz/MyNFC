package com.github.yohannestz.mynfc.nfc

fun ByteArray.toHex(separator: String = ""): String =
    joinToString(separator) { "%02X".format(it.toInt() and 0xFF) }

fun String.hexToBytes(): ByteArray {
    val clean = filter { it.isLetterOrDigit() }
    require(clean.length % 2 == 0) { "Odd hex length" }
    return ByteArray(clean.length / 2) { i -> clean.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
}

fun Int.hex2(): String = "0x%02X".format(this and 0xFF)

fun formatBytes(bytes: Int): String = when {
    bytes < 1024 -> "$bytes bytes"
    else -> "%.1f KB".format(bytes / 1024f)
}

/** IC manufacturer codes from ISO/IEC 7816-6 (first UID byte for ISO 14443-A tags). */
object Manufacturers {
    private val codes = mapOf(
        0x01 to "Motorola",
        0x02 to "STMicroelectronics",
        0x03 to "Hitachi",
        0x04 to "NXP Semiconductors",
        0x05 to "Infineon Technologies",
        0x06 to "Cylink",
        0x07 to "Texas Instruments",
        0x08 to "Fujitsu",
        0x09 to "Matsushita",
        0x0A to "NEC",
        0x0B to "Oki Electric",
        0x0C to "Toshiba",
        0x0D to "Mitsubishi Electric",
        0x0E to "Samsung Electronics",
        0x0F to "Hynix",
        0x10 to "LG Semiconductors",
        0x16 to "EM Microelectronic",
        0x1F to "Melexis",
        0x27 to "Broadcom",
        0x28 to "Mikron",
        0x2B to "Maxim",
        0x33 to "AMIC",
        0x39 to "Silicon Craft",
        0x44 to "Gentag",
        0x88 to "Infineon Technologies",
    )

    fun fromUid(uid: ByteArray): String? =
        if (uid.size >= 7) codes[uid[0].toInt() and 0xFF] else null
}
