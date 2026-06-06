package at.priv.graf.zazentimer

import android.app.Application
import android.os.Process
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ZazenTimerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (!applicationInfo.processName.endsWith(":crash")) {
            Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
                val intent = CrashActivity.createIntent(this, throwable)
                startActivity(intent)
                Process.killProcess(Process.myPid())
            }
        }
    }
}
