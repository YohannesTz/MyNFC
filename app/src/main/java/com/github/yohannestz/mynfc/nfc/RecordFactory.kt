package com.github.yohannestz.mynfc.nfc

import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import com.github.yohannestz.mynfc.data.model.RecordType
import com.github.yohannestz.mynfc.data.model.WriteRecord

/** Turns user-composed [WriteRecord]s into NDEF. */
object RecordFactory {

    fun toNdef(record: WriteRecord): NdefRecord = when (record.type) {
        RecordType.URL -> NdefRecord.createUri(normalizeUrl(record["url"]))
        RecordType.TEXT -> NdefRecord.createTextRecord(record["lang"].ifEmpty { "en" }, record["text"])
        RecordType.CONTACT -> NdefRecord.createMime("text/vcard", vCard(record).toByteArray())
        RecordType.PHONE -> NdefRecord.createUri("tel:${record["number"].filterPhone()}")
        RecordType.EMAIL -> NdefRecord.createUri(mailto(record))
        RecordType.SMS -> NdefRecord.createUri(sms(record))
        RecordType.WIFI -> NdefRecord.createMime(
            WifiCodec.MIME,
            WifiCodec.encode(record["ssid"], record["auth"].ifEmpty { "WPA/WPA2" }, record["password"]),
        )
        RecordType.LOCATION -> NdefRecord.createUri(geo(record))
        RecordType.APP -> NdefRecord.createApplicationRecord(record["package"])
        else -> NdefRecord.createUri(socialUrl(record.type, record["handle"]))
    }

    fun toMessage(records: List<WriteRecord>): NdefMessage =
        NdefMessage(records.map(::toNdef).toTypedArray())

    fun sizeOf(records: List<WriteRecord>): Int =
        runCatching { toMessage(records).byteArrayLength }.getOrDefault(0)

    /** Returns an error message, or null when the fields are valid for this record type. */
    fun validate(type: RecordType, fields: Map<String, String>): String? {
        type.fields.firstOrNull { it.required && fields[it.key].isNullOrBlank() }?.let {
            return "${it.label} is required"
        }
        val get = { key: String -> fields[key].orEmpty().trim() }
        return when (type) {
            RecordType.URL -> if (!get("url").contains('.') && !get("url").contains(':')) "Enter a valid URL" else null
            RecordType.EMAIL -> if (!get("to").contains('@')) "Enter a valid email address" else null
            RecordType.LOCATION -> {
                val lat = get("lat").toDoubleOrNull()
                val lng = get("lng").toDoubleOrNull()
                when {
                    lat == null || lat !in -90.0..90.0 -> "Latitude must be between -90 and 90"
                    lng == null || lng !in -180.0..180.0 -> "Longitude must be between -180 and 180"
                    else -> null
                }
            }
            RecordType.WIFI -> if (get("auth") != "Open" && get("password").length < 8) "Wi-Fi password must be at least 8 characters" else null
            RecordType.APP -> if (!get("package").contains('.')) "Enter a package name like com.example.app" else null
            else -> null
        }
    }

    fun summary(record: WriteRecord): String = when (record.type) {
        RecordType.URL -> normalizeUrl(record["url"])
        RecordType.TEXT -> record["text"]
        RecordType.CONTACT -> listOf(record["name"], record["org"]).filter { it.isNotEmpty() }.joinToString(" · ")
        RecordType.PHONE -> record["number"]
        RecordType.EMAIL -> record["to"]
        RecordType.SMS -> record["number"]
        RecordType.WIFI -> "${record["ssid"]} · ${record["auth"].ifEmpty { "WPA/WPA2" }}"
        RecordType.LOCATION -> record["label"].ifEmpty { "${record["lat"]}, ${record["lng"]}" }
        RecordType.APP -> record["package"]
        else -> socialUrl(record.type, record["handle"])
    }

    private fun normalizeUrl(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.contains("://") || trimmed.startsWith("mailto:") || trimmed.startsWith("tel:")) trimmed else "https://$trimmed"
    }

    private fun String.filterPhone() = filter { it.isDigit() || it == '+' }

    private fun mailto(r: WriteRecord): String {
        val params = listOfNotNull(
            r["subject"].takeIf { it.isNotEmpty() }?.let { "subject=${Uri.encode(it)}" },
            r["body"].takeIf { it.isNotEmpty() }?.let { "body=${Uri.encode(it)}" },
        )
        return "mailto:${r["to"]}" + if (params.isEmpty()) "" else "?" + params.joinToString("&")
    }

    private fun sms(r: WriteRecord): String {
        val body = r["body"].takeIf { it.isNotEmpty() }?.let { "?body=${Uri.encode(it)}" }.orEmpty()
        return "sms:${r["number"].filterPhone()}$body"
    }

    private fun geo(r: WriteRecord): String {
        val coords = "${r["lat"]},${r["lng"]}"
        val label = r["label"].takeIf { it.isNotEmpty() }?.let { "?q=$coords(${Uri.encode(it)})" }.orEmpty()
        return "geo:$coords$label"
    }

    private fun vCard(r: WriteRecord): String = buildString {
        appendLine("BEGIN:VCARD")
        appendLine("VERSION:3.0")
        appendLine("FN:${r["name"]}")
        val parts = r["name"].split(' ', limit = 2)
        appendLine("N:${parts.getOrElse(1) { "" }};${parts[0]};;;")
        if (r["org"].isNotEmpty()) appendLine("ORG:${r["org"]}")
        if (r["title"].isNotEmpty()) appendLine("TITLE:${r["title"]}")
        if (r["phone"].isNotEmpty()) appendLine("TEL;TYPE=CELL:${r["phone"]}")
        if (r["email"].isNotEmpty()) appendLine("EMAIL:${r["email"]}")
        if (r["website"].isNotEmpty()) appendLine("URL:${normalizeUrl(r["website"])}")
        if (r["address"].isNotEmpty()) appendLine("ADR:;;${r["address"]};;;;")
        append("END:VCARD")
    }

    private fun socialUrl(type: RecordType, rawHandle: String): String {
        val handle = rawHandle.trim().removePrefix("@")
        if (handle.startsWith("http")) return handle
        return when (type) {
            RecordType.INSTAGRAM -> "https://instagram.com/$handle"
            RecordType.LINKEDIN -> "https://linkedin.com/in/$handle"
            RecordType.TELEGRAM -> "https://t.me/$handle"
            RecordType.WHATSAPP -> "https://wa.me/${handle.filter { it.isDigit() }}"
            RecordType.YOUTUBE -> "https://youtube.com/@$handle"
            RecordType.X -> "https://x.com/$handle"
            RecordType.FACEBOOK -> "https://facebook.com/$handle"
            RecordType.TIKTOK -> "https://tiktok.com/@$handle"
            else -> handle
        }
    }
}
