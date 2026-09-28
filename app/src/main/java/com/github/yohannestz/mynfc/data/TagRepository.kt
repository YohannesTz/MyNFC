package com.github.yohannestz.mynfc.data

import android.content.Context
import com.github.yohannestz.mynfc.data.model.ExportBundle
import com.github.yohannestz.mynfc.data.model.ScannedTag
import com.github.yohannestz.mynfc.data.model.WriteRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Persists saved tags and composed records as JSON files in app storage.
 * The on-disk format doubles as the export format, so exports are lossless.
 */
class TagRepository(context: Context) {

    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val tagsFile = File(context.filesDir, "saved_tags.json")
    private val recordsFile = File(context.filesDir, "records.json")

    private val _savedTags = MutableStateFlow(load(tagsFile, ListSerializer(ScannedTag.serializer())))
    val savedTags: StateFlow<List<ScannedTag>> = _savedTags.asStateFlow()

    private val _records = MutableStateFlow(load(recordsFile, ListSerializer(WriteRecord.serializer())))
    val records: StateFlow<List<WriteRecord>> = _records.asStateFlow()

    /** Tags scanned this session but not (yet) saved, keyed by id. */
    private val _recent = MutableStateFlow<Map<String, ScannedTag>>(emptyMap())
    val recent: StateFlow<Map<String, ScannedTag>> = _recent.asStateFlow()

    fun remember(tag: ScannedTag) = _recent.update { it + (tag.id to tag) }

    fun findTag(id: String): ScannedTag? =
        _savedTags.value.firstOrNull { it.id == id } ?: _recent.value[id]

    fun saveTag(tag: ScannedTag) {
        _savedTags.update { list -> listOf(tag) + list.filterNot { it.id == tag.id } }
        persistTags()
    }

    fun deleteTag(id: String) {
        _savedTags.update { list -> list.filterNot { it.id == id } }
        persistTags()
    }

    fun upsertRecord(record: WriteRecord) {
        _records.update { list ->
            if (list.any { it.id == record.id }) list.map { if (it.id == record.id) record else it }
            else listOf(record) + list
        }
        persistRecords()
    }

    fun deleteRecords(ids: Set<String>) {
        _records.update { list -> list.filterNot { it.id in ids } }
        persistRecords()
    }

    fun exportJson(tags: List<ScannedTag> = savedTags.value, records: List<WriteRecord> = this.records.value): String =
        json.encodeToString(ExportBundle.serializer(), ExportBundle(exportedAt = System.currentTimeMillis(), tags = tags, records = records))

    /** Merges an export bundle into storage. Returns (tags, records) imported. */
    suspend fun importJson(text: String): Pair<Int, Int> = withContext(Dispatchers.Default) {
        val bundle = json.decodeFromString(ExportBundle.serializer(), text)
        val tagIds = _savedTags.value.map { it.id }.toSet()
        val recordIds = _records.value.map { it.id }.toSet()
        val newTags = bundle.tags.filterNot { it.id in tagIds }
        val newRecords = bundle.records.filterNot { it.id in recordIds }
        _savedTags.update { (newTags + it).sortedByDescending { t -> t.scannedAt } }
        _records.update { newRecords + it }
        persistTags()
        persistRecords()
        newTags.size to newRecords.size
    }

    private fun persistTags() = persist(tagsFile, ListSerializer(ScannedTag.serializer()), _savedTags::value)
    private fun persistRecords() = persist(recordsFile, ListSerializer(WriteRecord.serializer()), _records::value)

    private fun <T> persist(file: File, serializer: KSerializer<T>, latest: () -> T) {
        scope.launch {
            mutex.withLock {
                val tmp = File(file.parentFile, "${file.name}.tmp")
                tmp.writeText(json.encodeToString(serializer, latest()))
                tmp.renameTo(file)
            }
        }
    }

    private fun <T> load(file: File, serializer: KSerializer<List<T>>): List<T> =
        runCatching { json.decodeFromString(serializer, file.readText()) }.getOrDefault(emptyList())
}
