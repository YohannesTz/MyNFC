package com.github.yohannestz.mynfc.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import com.github.yohannestz.mynfc.data.model.NdefRecordInfo
import com.github.yohannestz.mynfc.data.model.RecordKind
import java.nio.charset.Charset

/** Decodes raw NDEF records into human readable [NdefRecordInfo]. */
object NdefParser {

    fun parse(message: NdefMessage?): List<NdefRecordInfo> =
        message?.records?.map(::parse).orEmpty()

    fun parse(record: NdefRecord): NdefRecordInfo {
        val type = record.type ?: ByteArray(0)
        val payload = record.payload ?: ByteArray(0)
        val typeText = type.decodeToString()
        val (kind, value, mime) = decode(record, type, payload)
        return NdefRecordInfo(
            kind = kind,
            tnf = record.tnf.toInt(),
            tnfName = tnfName(record.tnf),
            type = typeText,
            typeHex = type.toHex(),
            idHex = record.id?.toHex().orEmpty(),
            payloadHex = payload.toHex(),
            value = value,
            mimeType = mime,
            sizeBytes = record.toByteArray().size,
        )
    }

    /** Rebuilds the exact on-tag record from stored info (used for cloning saved tags). */
    fun toRecord(info: NdefRecordInfo): NdefRecord = NdefRecord(
        info.tnf.toShort(),
        info.typeHex.hexToBytes(),
        info.idHex.hexToBytes(),
        info.payloadHex.hexToBytes(),
    )

    private fun decode(record: NdefRecord, type: ByteArray, payload: ByteArray): Triple<RecordKind, String, String?> {
        return when (record.tnf) {
            NdefRecord.TNF_EMPTY -> Triple(RecordKind.EMPTY, "Empty record", null)
            NdefRecord.TNF_WELL_KNOWN -> when {
                type.contentEquals(NdefRecord.RTD_URI) -> uriTriple(record.toUri()?.toString().orEmpty())
                type.contentEquals(NdefRecord.RTD_TEXT) -> Triple(RecordKind.TEXT, decodeText(payload), "text/plain")
                type.contentEquals(NdefRecord.RTD_SMART_POSTER) -> smartPoster(payload)
                else -> Triple(RecordKind.UNKNOWN, payload.readableOrHex(), null)
            }
            NdefRecord.TNF_ABSOLUTE_URI -> uriTriple(type.decodeToString())
            NdefRecord.TNF_MIME_MEDIA -> mime(type.decodeToString().lowercase(), payload)
            NdefRecord.TNF_EXTERNAL_TYPE -> {
                val domain = type.decodeToString()
                if (domain == "android.com:pkg") Triple(RecordKind.APP, payload.decodeToString(), null)
                else Triple(RecordKind.EXTERNAL, "$domain\n${payload.readableOrHex()}", null)
            }
            else -> Triple(RecordKind.UNKNOWN, payload.readableOrHex(), null)
        }
    }

    private fun uriTriple(uri: String): Triple<RecordKind, String, String?> {
        val lower = uri.lowercase()
        val kind = when {
            lower.startsWith("tel:") -> RecordKind.PHONE
            lower.startsWith("mailto:") -> RecordKind.EMAIL
            lower.startsWith("sms:") || lower.startsWith("smsto:") -> RecordKind.SMS
            lower.startsWith("geo:") -> RecordKind.GEO
            else -> RecordKind.URL
        }
        return Triple(kind, uri, null)
    }

    private fun smartPoster(payload: ByteArray): Triple<RecordKind, String, String?> {
        val inner = runCatching { NdefMessage(payload) }.getOrNull()
            ?: return Triple(RecordKind.SMART_POSTER, payload.readableOrHex(), null)
        val parts = inner.records.map(::parse)
        val uri = parts.firstOrNull { it.kind in LINK_KINDS }?.value
        val title = parts.firstOrNull { it.kind == RecordKind.TEXT }?.value
        return Triple(RecordKind.SMART_POSTER, listOfNotNull(title, uri).joinToString("\n"), null)
    }

    private fun mime(mime: String, payload: ByteArray): Triple<RecordKind, String, String?> = when {
        mime == WifiCodec.MIME -> {
            val cred = WifiCodec.decode(payload)
            val text = cred?.let { "SSID: ${it.ssid}\nSecurity: ${it.auth}" + if (it.password.isNotEmpty()) "\nPassword: ${it.password}" else "" }
            Triple(RecordKind.WIFI, text ?: payload.toHex(" "), mime)
        }
        mime == "text/vcard" || mime == "text/x-vcard" -> Triple(RecordKind.CONTACT, summarizeVCard(payload.decodeToString()), mime)
        mime.startsWith("text/") || mime.endsWith("json") || mime.endsWith("xml") ->
            Triple(RecordKind.MIME, payload.decodeToString(), mime)
        else -> Triple(RecordKind.MIME, payload.readableOrHex(), mime)
    }

    private fun decodeText(payload: ByteArray): String {
        if (payload.isEmpty()) return ""
        val status = payload[0].toInt()
        val charset = if (status and 0x80 == 0) Charsets.UTF_8 else Charset.forName("UTF-16")
        val langLength = status and 0x3F
        if (1 + langLength > payload.size) return payload.decodeToString()
        return String(payload, 1 + langLength, payload.size - 1 - langLength, charset)
    }

    private fun summarizeVCard(vcard: String): String {
        val labels = mapOf("FN" to "Name", "ORG" to "Company", "TITLE" to "Title", "TEL" to "Phone", "EMAIL" to "Email", "URL" to "Web", "ADR" to "Address")
        val lines = vcard.lines().mapNotNull { line ->
            val key = line.substringBefore(':').substringBefore(';').uppercase()
            val label = labels[key] ?: return@mapNotNull null
            val value = line.substringAfter(':').split(';').filter { it.isNotBlank() }.joinToString(", ")
            "$label: $value".takeIf { value.isNotBlank() }
        }
        return lines.joinToString("\n").ifEmpty { vcard }
    }

    private fun ByteArray.readableOrHex(): String {
        if (isEmpty()) return "(no payload)"
        val text = decodeToString()
        val printable = text.count { !it.isISOControl() || it == '\n' || it == '\r' || it == '\t' }
        return if (text.isNotEmpty() && printable >= text.length * 0.9 && '�' !in text) text else toHex(" ")
    }

    fun tnfName(tnf: Short): String = when (tnf) {
        NdefRecord.TNF_EMPTY -> "Empty"
        NdefRecord.TNF_WELL_KNOWN -> "NFC Well Known"
        NdefRecord.TNF_MIME_MEDIA -> "Media type"
        NdefRecord.TNF_ABSOLUTE_URI -> "Absolute URI"
        NdefRecord.TNF_EXTERNAL_TYPE -> "NFC External"
        NdefRecord.TNF_UNKNOWN -> "Unknown"
        NdefRecord.TNF_UNCHANGED -> "Unchanged"
        else -> "Reserved"
    }

    val LINK_KINDS = setOf(RecordKind.URL, RecordKind.PHONE, RecordKind.EMAIL, RecordKind.SMS, RecordKind.GEO)
}
