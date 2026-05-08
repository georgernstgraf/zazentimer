package at.priv.graf.zazentimer.views

import android.animation.Animator
import android.animation.ObjectAnimator
import android.app.Activity
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import at.priv.graf.zazentimer.R

class MessageView(private val activity: Activity) {

    private var contentText: String? = null
    private var messageParent: ViewGroup? = null
    private var messageView: ViewGroup? = null
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
        if (this.messageView != null) {
            Log.d(TAG, "message already visible")
            return
        }
        this.messageParent = activity.findViewById(android.R.id.content) as ViewGroup
        this.messageView = activity.layoutInflater.inflate(R.layout.message_view, this.messageParent, false) as ViewGroup
        if (Build.VERSION.SDK_INT >= 21) {
            this.messageView!!.elevation = 50.0f
        }
        val textView = this.messageView!!.findViewById<TextView>(R.id.message_title)
        val textView2 = this.messageView!!.findViewById<TextView>(R.id.message_text)
        val button = this.messageView!!.findViewById<Button>(R.id.message_ok)
        textView.text = this.titleText
        textView2.text = this.contentText
        this.messageView!!.alpha = 0.0f
        val objectAnimator = ObjectAnimator()
        objectAnimator.target = this.messageView
        objectAnimator.setPropertyName("alpha")
        objectAnimator.setFloatValues(0.0f, 1.0f)
        objectAnimator.duration = 500L
        objectAnimator.start()
        this.messageParent!!.addView(this.messageView)
        button.setOnClickListener {
            val objectAnimator2 = ObjectAnimator()
            objectAnimator2.target = this@MessageView.messageView
            objectAnimator2.setPropertyName("alpha")
            objectAnimator2.setFloatValues(1.0f, 0.0f)
            objectAnimator2.duration = 500L
            objectAnimator2.addListener(object : Animator.AnimatorListener {
                override fun onAnimationCancel(animator: Animator) {
                }

                override fun onAnimationRepeat(animator: Animator) {
                }

                override fun onAnimationStart(animator: Animator) {
                }

                override fun onAnimationEnd(animator: Animator) {
                    this@MessageView.messageParent!!.removeView(this@MessageView.messageView)
                    this@MessageView.messageView = null
                    this@MessageView.onOkListener?.run()
                }
            })
            objectAnimator2.start()
        }
    }

    companion object {
        private const val TAG = "ZMT_MessageView"
    }
}
