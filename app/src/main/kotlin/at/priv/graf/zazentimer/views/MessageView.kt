package at.priv.graf.zazentimer.views

import android.animation.Animator
import android.animation.ObjectAnimator
import android.app.Activity
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import at.priv.graf.zazentimer.R

class MessageView(
    private val activity: Activity,
) {
    private var contentText: String? = null
    private var messageParent: ViewGroup? = null
    private var messageViewContainer: ViewGroup? = null
    private var onOkListener: Runnable? = null
    private var titleText: String? = null

    fun setTitle(str: String) {
        this.titleText = str
    }

    fun setText(str: String) {
        this.contentText = str
    }

    fun setOnOkListener(runnable: Runnable) {
        this.onOkListener = runnable
    }

    fun show() {
        if (this.messageViewContainer != null) {
            Log.d(TAG, "message already visible")
            return
        }
        val parent = activity.findViewById(android.R.id.content) as? ViewGroup
        val view = parent?.let { activity.layoutInflater.inflate(R.layout.message_view, it, false) as? ViewGroup }
        if (parent == null || view == null) return
        this.messageParent = parent
        this.messageViewContainer = view
        view.elevation = ELEVATION_DP
        val textView = view.findViewById<TextView>(R.id.message_title)
        val textView2 = view.findViewById<TextView>(R.id.message_text)
        val button = view.findViewById<Button>(R.id.message_ok)
        textView.text = this.titleText
        textView2.text = this.contentText
        view.alpha = 0.0f
        val objectAnimator = ObjectAnimator()
        objectAnimator.target = view
        objectAnimator.setPropertyName("alpha")
        objectAnimator.setFloatValues(0.0f, 1.0f)
        objectAnimator.duration = ANIM_DURATION_MS
        objectAnimator.start()
        parent.addView(view)
        button.setOnClickListener {
            val objectAnimator2 = ObjectAnimator()
            objectAnimator2.target = this@MessageView.messageViewContainer
            objectAnimator2.setPropertyName("alpha")
            objectAnimator2.setFloatValues(1.0f, 0.0f)
            objectAnimator2.duration = ANIM_DURATION_MS
            objectAnimator2.addListener(
                object : Animator.AnimatorListener {
                    override fun onAnimationCancel(animator: Animator) {
                        // no-op: animation lifecycle
                    }

                    override fun onAnimationRepeat(animator: Animator) {
                        // no-op: animation lifecycle
                    }

                    override fun onAnimationStart(animator: Animator) {
                        // no-op: animation lifecycle
                    }

                    override fun onAnimationEnd(animator: Animator) {
                        this@MessageView.messageParent?.removeView(
                            this@MessageView.messageViewContainer,
                        )
                        this@MessageView.messageViewContainer = null
                        this@MessageView.onOkListener?.run()
                    }
                },
            )
            objectAnimator2.start()
        }
    }

    companion object {
        private const val TAG = "ZMT_MessageView"
        private const val ELEVATION_DP = 50.0f
        private const val ANIM_DURATION_MS = 500L
    }
}
