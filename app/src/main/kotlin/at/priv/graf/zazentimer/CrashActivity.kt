package at.priv.graf.zazentimer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class CrashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val exceptionType = intent.getStringExtra(EXTRA_EXCEPTION_TYPE) ?: "Unknown"
        val exceptionMessage = intent.getStringExtra(EXTRA_EXCEPTION_MESSAGE) ?: ""
        val stackTrace = intent.getStringExtra(EXTRA_STACK_TRACE) ?: ""

        val title = getString(R.string.crash_dialog_title)
        val message =
            buildString {
                append(exceptionType)
                if (exceptionMessage.isNotBlank()) {
                    append("\n\n")
                    append(exceptionMessage)
                }
                val frames = stackTrace.lines().take(MAX_VISIBLE_FRAMES)
                if (frames.isNotEmpty()) {
                    append("\n\n")
                    append(frames.joinToString("\n"))
                }
            }

        val dialog =
            AlertDialog
                .Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.crash_copy_exit) { _, _ ->
                    copyToClipboard(stackTrace)
                    exitApp()
                }.setNegativeButton(R.string.crash_exit) { _, _ ->
                    exitApp()
                }.setCancelable(false)
                .create()

        dialog.show()

        findViewById<TextView>(android.R.id.message)?.let {
            it.setTextIsSelectable(true)
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("stack_trace", text)
        clipboard.setPrimaryClip(clip)
    }

    private fun exitApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask()
        } else {
            finish()
        }
        System.exit(1)
    }

    companion object {
        private const val EXTRA_EXCEPTION_TYPE = "exception_type"
        private const val EXTRA_EXCEPTION_MESSAGE = "exception_message"
        private const val EXTRA_STACK_TRACE = "stack_trace"
        private const val MAX_VISIBLE_FRAMES = 10

        fun createIntent(
            context: Context,
            throwable: Throwable,
        ): Intent =
            Intent(context, CrashActivity::class.java).apply {
                putExtra(EXTRA_EXCEPTION_TYPE, throwable.javaClass.name)
                putExtra(EXTRA_EXCEPTION_MESSAGE, throwable.message ?: "")
                putExtra(EXTRA_STACK_TRACE, Log.getStackTraceString(throwable))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
    }
}
