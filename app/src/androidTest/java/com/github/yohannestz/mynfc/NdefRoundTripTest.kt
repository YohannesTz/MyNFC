package com.github.yohannestz.mynfc

import android.nfc.NdefMessage
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.yohannestz.mynfc.data.model.ExportBundle
import com.github.yohannestz.mynfc.data.model.RecordKind
import com.github.yohannestz.mynfc.data.model.RecordType
import com.github.yohannestz.mynfc.data.model.ScannedTag
import com.github.yohannestz.mynfc.data.model.WriteRecord
import com.github.yohannestz.mynfc.nfc.NdefParser
import com.github.yohannestz.mynfc.nfc.RecordFactory
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NdefRoundTripTest {

    private fun record(type: RecordType, vararg fields: Pair<String, String>) =
        WriteRecord("id", type, mapOf(*fields), 0L)

    /** Encodes through the real NdefMessage byte format and parses it back. */
    private fun roundTrip(vararg records: WriteRecord) =
        NdefParser.parse(NdefMessage(RecordFactory.toMessage(records.toList()).toByteArray()))

    @Test
    fun url() {
        val parsed = roundTrip(record(RecordType.URL, "url" to "example.com/path")).single()
        assertEquals(RecordKind.URL, parsed.kind)
        assertEquals("https://example.com/path", parsed.value)
    }

    @Test
    fun text() {
        val parsed = roundTrip(record(RecordType.TEXT, "text" to "Selam 👋", "lang" to "am")).single()
        assertEquals(RecordKind.TEXT, parsed.kind)
        assertEquals("Selam 👋", parsed.value)
    }

    @Test
    fun wifi() {
        val parsed = roundTrip(record(RecordType.WIFI, "ssid" to "OfficeNet", "auth" to "WPA2", "password" to "supersecret1")).single()
        assertEquals(RecordKind.WIFI, parsed.kind)
        assertTrue(parsed.value, parsed.value.contains("SSID: OfficeNet"))
        assertTrue(parsed.value.contains("Security: WPA2"))
        assertTrue(parsed.value.contains("Password: supersecret1"))
    }

    @Test
    fun contact() {
        val parsed = roundTrip(record(RecordType.CONTACT, "name" to "Abebe Kebede", "phone" to "+251911000000", "org" to "Acme")).single()
        assertEquals(RecordKind.CONTACT, parsed.kind)
        assertTrue(parsed.value, parsed.value.contains("Name: Abebe Kebede"))
        assertTrue(parsed.value.contains("Phone: +251911000000"))
    }

    @Test
    fun phoneEmailSmsGeoApp() {
        val parsed = roundTrip(
            record(RecordType.PHONE, "number" to "+251 911 000 000"),
            record(RecordType.EMAIL, "to" to "a@b.com", "subject" to "Hi there"),
            record(RecordType.SMS, "number" to "0911", "body" to "yo"),
            record(RecordType.LOCATION, "lat" to "9.01", "lng" to "38.76"),
            record(RecordType.APP, "package" to "com.android.chrome"),
            record(RecordType.INSTAGRAM, "handle" to "@me"),
        )
        assertEquals(listOf(RecordKind.PHONE, RecordKind.EMAIL, RecordKind.SMS, RecordKind.GEO, RecordKind.APP, RecordKind.URL), parsed.map { it.kind })
        assertEquals("tel:+251911000000", parsed[0].value)
        assertEquals("mailto:a@b.com?subject=Hi%20there", parsed[1].value)
        assertEquals("geo:9.01,38.76", parsed[3].value)
        assertEquals("com.android.chrome", parsed[4].value)
        assertEquals("https://instagram.com/me", parsed[5].value)
    }

    @Test
    fun cloneRebuildsIdenticalBytes() {
        val original = RecordFactory.toMessage(listOf(record(RecordType.URL, "url" to "https://x.com"), record(RecordType.TEXT, "text" to "hi")))
        val rebuilt = NdefMessage(NdefParser.parse(original).map(NdefParser::toRecord).toTypedArray())
        assertTrue(original.toByteArray().contentEquals(rebuilt.toByteArray()))
    }

    @Test
    fun validation() {
        assertNotNull(RecordFactory.validate(RecordType.URL, emptyMap()))
        assertNotNull(RecordFactory.validate(RecordType.WIFI, mapOf("ssid" to "x", "auth" to "WPA2", "password" to "short")))
        assertNull(RecordFactory.validate(RecordType.WIFI, mapOf("ssid" to "x", "auth" to "Open")))
        assertNotNull(RecordFactory.validate(RecordType.LOCATION, mapOf("lat" to "95", "lng" to "0")))
    }

    @Test
    fun exportBundleRoundTrip() {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val tag = ScannedTag(
            id = "t1", scannedAt = 1L, uid = "04:A2", tagType = "NTAG215",
            records = NdefParser.parse(RecordFactory.toMessage(listOf(record(RecordType.URL, "url" to "a.com")))),
        )
        val bundle = ExportBundle(exportedAt = 2L, tags = listOf(tag), records = listOf(record(RecordType.TEXT, "text" to "x")))
        val decoded = json.decodeFromString(ExportBundle.serializer(), json.encodeToString(ExportBundle.serializer(), bundle))
        assertEquals(bundle, decoded)
    }
}
