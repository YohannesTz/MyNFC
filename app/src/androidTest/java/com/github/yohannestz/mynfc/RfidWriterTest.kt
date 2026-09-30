package com.github.yohannestz.mynfc

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.yohannestz.mynfc.data.model.RfidFamily
import com.github.yohannestz.mynfc.nfc.RawTagWriter
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RfidWriterTest {
    @Test fun unitSizesMatchTech() {
        assertEquals(16, RawTagWriter.unitSize(RfidFamily.MIFARE_CLASSIC))
        assertEquals(4, RawTagWriter.unitSize(RfidFamily.ULTRALIGHT))
        assertEquals(4, RawTagWriter.unitSize(RfidFamily.ISO15693))
        assertEquals(0, RawTagWriter.unitSize(RfidFamily.UNKNOWN))
    }
}
