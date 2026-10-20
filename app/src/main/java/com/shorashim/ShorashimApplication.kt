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
        // New chat message notifications: fast checks while the process is alive, plus a periodic background job
        // that keeps working after Android closes the app.
        com.shorashim.core.notifications.ChatMessageNotifier.startInAppChecks(this, container)
        com.shorashim.core.notifications.ChatNotificationWorker.schedule(this)
    }

    companion object {
        lateinit var instance: ShorashimApplication
            private set
    }
}
