package com.github.yohannestz.mynfc.data.model

import kotlinx.serialization.Serializable

/** Snapshot of everything we could read from a physical tag. */
@Serializable
data class ScannedTag(
    val id: String,
    val scannedAt: Long,
    val name: String? = null,
    val uid: String,
    val tagType: String,
    val technologies: List<String> = emptyList(),
    val manufacturer: String? = null,
    val ndefType: String? = null,
    val maxSize: Int? = null,
    val usedSize: Int? = null,
    val writable: Boolean? = null,
    val canMakeReadOnly: Boolean? = null,
    val formatable: Boolean = false,
    val records: List<NdefRecordInfo> = emptyList(),
    val chipInfo: List<InfoEntry> = emptyList(),
) {
    val displayName: String get() = name?.takeIf { it.isNotBlank() } ?: tagType
    val hasNdefData: Boolean get() = records.any { it.kind != RecordKind.EMPTY }
    val supportsNdef: Boolean get() = ndefType != null
}

@Serializable
data class InfoEntry(val label: String, val value: String)

@Serializable
data class NdefRecordInfo(
    val kind: RecordKind,
    val tnf: Int,
    val tnfName: String,
    val type: String,
    val typeHex: String,
    val idHex: String = "",
    val payloadHex: String,
    val value: String,
    val mimeType: String? = null,
    val sizeBytes: Int,
)

@Serializable
enum class RecordKind(val label: String) {
    URL("Link"),
    TEXT("Text"),
    PHONE("Phone"),
    EMAIL("Email"),
    SMS("SMS"),
    GEO("Location"),
    WIFI("Wi-Fi"),
    CONTACT("Contact"),
    APP("App"),
    SMART_POSTER("Smart Poster"),
    MIME("Data"),
    EXTERNAL("External"),
    EMPTY("Empty"),
    UNKNOWN("Unknown"),
}
