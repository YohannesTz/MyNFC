package com.github.yohannestz.mynfc.nfc

import java.io.ByteArrayOutputStream

/** Wi-Fi Simple Configuration (WSC) credential encoding used by "application/vnd.wfa.wsc" records. */
object WifiCodec {
    const val MIME = "application/vnd.wfa.wsc"

    private const val CREDENTIAL = 0x100E
    private const val NETWORK_INDEX = 0x1026
    private const val SSID = 0x1045
    private const val AUTH_TYPE = 0x1003
    private const val ENCRYPTION_TYPE = 0x100F
    private const val NETWORK_KEY = 0x1027
    private const val MAC_ADDRESS = 0x1020

    private const val AUTH_OPEN = 0x0001
    private const val AUTH_WPA_PSK = 0x0002
    private const val AUTH_WPA2_PSK = 0x0020
    private const val ENC_NONE = 0x0001
    private const val ENC_TKIP = 0x0004
    private const val ENC_AES = 0x0008

    data class Credential(val ssid: String, val auth: String, val password: String)

    fun encode(ssid: String, auth: String, password: String): ByteArray {
        val (authType, encType) = when (auth) {
            "Open" -> AUTH_OPEN to ENC_NONE
            "WPA" -> AUTH_WPA_PSK to ENC_TKIP
            "WPA2" -> AUTH_WPA2_PSK to ENC_AES
            else -> (AUTH_WPA_PSK or AUTH_WPA2_PSK) to (ENC_TKIP or ENC_AES)
        }
        val credential = ByteArrayOutputStream().apply {
            tlv(NETWORK_INDEX, byteArrayOf(1))
            tlv(SSID, ssid.toByteArray())
            tlv(AUTH_TYPE, short(authType))
            tlv(ENCRYPTION_TYPE, short(encType))
            tlv(NETWORK_KEY, if (auth == "Open") ByteArray(0) else password.toByteArray())
            tlv(MAC_ADDRESS, ByteArray(6) { 0xFF.toByte() })
        }.toByteArray()
        return ByteArrayOutputStream().apply { tlv(CREDENTIAL, credential) }.toByteArray()
    }

    fun decode(payload: ByteArray): Credential? {
        val outer = parse(payload)
        val inner = outer[CREDENTIAL]?.let(::parse) ?: outer
        val ssid = inner[SSID]?.decodeToString() ?: return null
        val authType = inner[AUTH_TYPE]?.let { ((it[0].toInt() and 0xFF) shl 8) or (it[1].toInt() and 0xFF) }
        val auth = when {
            authType == null || authType == AUTH_OPEN -> "Open"
            authType and AUTH_WPA2_PSK != 0 && authType and AUTH_WPA_PSK != 0 -> "WPA/WPA2"
            authType and AUTH_WPA2_PSK != 0 -> "WPA2"
            else -> "WPA"
        }
        return Credential(ssid, auth, inner[NETWORK_KEY]?.decodeToString().orEmpty())
    }

    private fun parse(data: ByteArray): Map<Int, ByteArray> {
        val out = mutableMapOf<Int, ByteArray>()
        var i = 0
        while (i + 4 <= data.size) {
            val type = ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            val len = ((data[i + 2].toInt() and 0xFF) shl 8) or (data[i + 3].toInt() and 0xFF)
            if (i + 4 + len > data.size) break
            out[type] = data.copyOfRange(i + 4, i + 4 + len)
            i += 4 + len
        }
        return out
    }

    private fun short(v: Int) = byteArrayOf((v shr 8).toByte(), v.toByte())

    private fun ByteArrayOutputStream.tlv(type: Int, value: ByteArray) {
        write(short(type))
        write(short(value.size))
        write(value)
    }
}
