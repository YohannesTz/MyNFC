package com.github.yohannestz.mynfc

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.IntentCompat
import com.github.yohannestz.mynfc.ui.MyNfcAppRoot
import com.github.yohannestz.mynfc.ui.theme.MyNFCTheme
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private val container by lazy { (application as MyNfcApplication).container }
    private val tagExecutor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyNFCTheme {
                MyNfcAppRoot(container)
            }
        }
        if (savedInstanceState == null) handleTagIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleTagIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // Reader mode keeps every tag inside the app (no system chooser) while we're in the foreground.
        NfcAdapter.getDefaultAdapter(this)?.enableReaderMode(
            this,
            { tag -> container.nfcController.onTagDiscovered(tag) },
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_NFC_BARCODE,
            Bundle().apply { putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250) },
        )
    }

    override fun onPause() {
        super.onPause()
        NfcAdapter.getDefaultAdapter(this)?.disableReaderMode(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        tagExecutor.shutdown()
    }

    /** Tags that launched the app from the background arrive via intent. */
    private fun handleTagIntent(intent: Intent?) {
        if (intent?.action !in TAG_ACTIONS) return
        val tag = IntentCompat.getParcelableExtra(intent!!, NfcAdapter.EXTRA_TAG, Tag::class.java) ?: return
        tagExecutor.execute { container.nfcController.onTagDiscovered(tag) }
    }

    private companion object {
        val TAG_ACTIONS = setOf(
            NfcAdapter.ACTION_NDEF_DISCOVERED,
            NfcAdapter.ACTION_TECH_DISCOVERED,
            NfcAdapter.ACTION_TAG_DISCOVERED,
        )
    }
}
