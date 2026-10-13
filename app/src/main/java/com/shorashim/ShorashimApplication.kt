package com.shorashim

import android.app.Application
import android.content.Context
import com.shorashim.core.common.LocaleHelper
import com.shorashim.data.repositories.AppContainer

class ShorashimApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = AppContainer(this)
    }

    companion object {
        lateinit var instance: ShorashimApplication
            private set
    }
}
