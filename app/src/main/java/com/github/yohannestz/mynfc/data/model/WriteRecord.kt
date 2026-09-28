package com.github.yohannestz.mynfc.data.model

import kotlinx.serialization.Serializable

/** A record the user composed and can write to a tag. Field keys are defined by [RecordType]. */
@Serializable
data class WriteRecord(
    val id: String,
    val type: RecordType,
    val fields: Map<String, String>,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
) {
    operator fun get(key: String): String = fields[key].orEmpty().trim()
}

@Serializable
data class ExportBundle(
    val app: String = "MyNFC",
    val formatVersion: Int = 1,
    val exportedAt: Long,
    val tags: List<ScannedTag> = emptyList(),
    val records: List<WriteRecord> = emptyList(),
)
