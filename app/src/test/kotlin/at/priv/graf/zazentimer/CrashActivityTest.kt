package at.priv.graf.zazentimer

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class CrashActivityTest {
    @Test
    fun crashDialog_displaysExceptionDetails() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val exception = IllegalStateException("Simulated fatal crash")
        val intent = CrashActivity.createIntent(context, exception)

        val controller = Robolectric.buildActivity(CrashActivity::class.java, intent)
        controller.create().start().resume()

        val dialog = ShadowDialog.getLatestDialog()
        assertThat(dialog).isNotNull()
        assertThat(dialog.isShowing).isTrue()

        val decorView = dialog.window?.decorView
        assertThat(decorView).isNotNull()

        val titleView = findTextViewContaining(decorView!!, "Fatal Error")
        assertThat(titleView).isNotNull()

        val exceptionView = findTextViewContaining(decorView, "java.lang.IllegalStateException")
        assertThat(exceptionView).isNotNull()

        val messageView = findTextViewContaining(decorView, "Simulated fatal crash")
        assertThat(messageView).isNotNull()
    }

    @Test
    fun createIntent_populatesExtras() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val exception = RuntimeException("Test error")

        val intent = CrashActivity.createIntent(context, exception)

        assertThat(intent.getStringExtra("exception_type"))
            .isEqualTo("java.lang.RuntimeException")

        assertThat(intent.getStringExtra("exception_message"))
            .isEqualTo("Test error")

        val stackTrace = intent.getStringExtra("stack_trace")
        assertThat(stackTrace).isNotNull()
        assertThat(stackTrace).contains("java.lang.RuntimeException")
        assertThat(stackTrace).contains("Test error")
        assertThat(stackTrace).contains("CrashActivityTest")
    }

    private fun findTextViewContaining(
        parent: View,
        text: String,
    ): TextView? {
        if (parent is TextView && parent.text.toString().contains(text)) {
            return parent
        }
        var result: TextView? = null
        if (parent is ViewGroup) {
            var i = 0
            while (i < parent.childCount && result == null) {
                result = findTextViewContaining(parent.getChildAt(i), text)
                i++
            }
        }
        return result
    }
}
