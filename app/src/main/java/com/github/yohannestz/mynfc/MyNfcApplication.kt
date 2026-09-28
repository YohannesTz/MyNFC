package com.github.yohannestz.mynfc

import android.app.Application
import com.github.yohannestz.mynfc.data.TagRepository
import com.github.yohannestz.mynfc.nfc.NfcController

class MyNfcApplication : Application() {
    val container by lazy { AppContainer(this) }
}

class AppContainer(application: Application) {
    val repository = TagRepository(application)
    val nfcController = NfcController(repository)
}
