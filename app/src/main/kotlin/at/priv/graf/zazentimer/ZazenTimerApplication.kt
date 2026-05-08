package at.priv.graf.zazentimer

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ZazenTimerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
