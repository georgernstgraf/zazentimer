package at.priv.graf.zazentimer.utils

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class DevicePreFlightRule : TestRule {
    override fun apply(
        base: Statement,
        description: Description,
    ): Statement =
        object : Statement() {
            override fun evaluate() {
                execute()
                base.evaluate()
            }
        }

    companion object {
        fun execute() {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            device.wakeUp()
            device.pressMenu()
            device.pressHome()
            try {
                device.executeShellCommand("settings put global window_animation_scale 0.0")
                device.executeShellCommand("settings put global transition_animation_scale 0.0")
                device.executeShellCommand("settings put global animator_duration_scale 0.0")
                device.executeShellCommand("svc power stayon true")
            } catch (_: Exception) {
            }
        }
    }
}
