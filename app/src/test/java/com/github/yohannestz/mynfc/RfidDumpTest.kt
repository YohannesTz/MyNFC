package com.github.yohannestz.mynfc

import com.github.yohannestz.mynfc.data.model.MemoryBlock
import com.github.yohannestz.mynfc.data.model.RfidBundle
import com.github.yohannestz.mynfc.data.model.RfidDump
import com.github.yohannestz.mynfc.data.model.RfidFamily
import com.github.yohannestz.mynfc.nfc.hexToBytes
import com.github.yohannestz.mynfc.nfc.toHex
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Device-independent RFID logic (hex codec, models, JSON export). Runs on the JVM. */
class RfidDumpTest {

    @Test fun hexRoundTrip() {
        val hex = "DEADBEEF0011223344556677889900AA"
        assertEquals(hex, hex.hexToBytes().toHex())
        assertEquals(16, hex.hexToBytes().size)
    }

    @Test fun hexIgnoresSeparators() {
        assertEquals("04A23B", "04:A2:3B".hexToBytes().toHex())
        assertEquals("DEAD", "DE AD".hexToBytes().toHex())
    }

    @Test fun blockSizeDerivedFromHex() {
        assertEquals(16, MemoryBlock(1, null, "00".repeat(16)).sizeBytes)
        assertEquals(4, MemoryBlock(1, null, "00".repeat(4)).sizeBytes)
    }

    @Test fun dumpBundleRoundTrip() {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val dump = RfidDump(
            id = "d1", scannedAt = 5L, name = "Office badge", family = RfidFamily.MIFARE_CLASSIC,
            uid = "5E:A1:07:C3", tagType = "MIFARE Classic 1K", totalBytes = 1024, blockSize = 16,
            sectorsRead = 3, sectorsTotal = 16, keysUsed = listOf("FFFFFFFFFFFF"),
            blocks = listOf(
                MemoryBlock(0, 0, "5EA107C3000000000000000000000000", writable = false, role = "manufacturer"),
                MemoryBlock(1, 0, "00000000000000000000000000000000"),
                MemoryBlock(3, 0, "FFFFFFFFFFFFFF078069FFFFFFFFFFFF", role = "sector-trailer"),
                MemoryBlock(8, 2, "00000000000000000000000000000000", readable = false, writable = false, note = "Custom key"),
            ),
        )
        val bundle = RfidBundle(exportedAt = 9L, dumps = listOf(dump))
        val out = json.decodeFromString(RfidBundle.serializer(), json.encodeToString(RfidBundle.serializer(), bundle))
        assertEquals(bundle, out)
        assertEquals(3, out.dumps[0].readableBlocks)
        assertTrue(out.dumps[0].partial)
    }
}
