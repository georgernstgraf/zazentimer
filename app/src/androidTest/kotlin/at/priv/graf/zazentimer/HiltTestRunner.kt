package at.priv.graf.zazentimer

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import at.priv.graf.zazentimer.utils.DevicePreFlightRule
import dagger.hilt.android.testing.HiltTestApplication

class HiltTestRunner : AndroidJUnitRunner() {
    override fun onStart() {
        try {
            DevicePreFlightRule.execute()
        } catch (_: Exception) {
        }
        super.onStart()
    }

    @Throws(ClassNotFoundException::class, IllegalAccessException::class, InstantiationException::class)
    override fun newApplication(
        cl: ClassLoader,
        className: String,
        context: Context,
    ): Application = super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
