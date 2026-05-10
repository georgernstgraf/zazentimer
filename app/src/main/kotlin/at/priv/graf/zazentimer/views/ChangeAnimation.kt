package at.priv.graf.zazentimer.views

import android.view.View
import java.util.HashMap

class MyAnimator {
    var duration = 0
    var fromVal = 0f
    var propertyIdx = 0
    var runOnEnd: Runnable? = null
    var startTime: Long = 0L
    var toVal = 0f
}

internal class TimerAnimator(
    private val view: View,
    val runningAnims: HashMap<Int, MyAnimator>,
    val floatProperties: FloatArray,
    private val animDuration: Int,
) {
    @Volatile
    var animating = false

    fun animateTo(
        index: Int,
        value: Int,
        duration: Int,
        onEnd: Runnable? = null,
    ) {
        var anim = runningAnims[index]
        if (anim == null) {
            anim = MyAnimator()
        }
        anim.runOnEnd = onEnd
        anim.propertyIdx = index
        anim.duration = duration
        anim.toVal = value.toFloat()
        anim.fromVal = floatProperties[index]
        anim.startTime = System.currentTimeMillis()
        runningAnims[index] = anim
        if (!animating) {
            startAnimation()
        }
    }

    fun animateTo(
        index: Int,
        value: Int,
    ) {
        animateTo(index, value, animDuration, null)
    }

    private fun startAnimation() {
        animating = true
        view.postDelayed(
            AnimationRunner(runningAnims, floatProperties, ::onAnimationStep),
            ANIM_POST_DELAY_MS,
        )
    }

    private fun onAnimationStep(hasActive: Boolean) {
        view.postInvalidate()
        if (hasActive) {
            view.postDelayed(
                AnimationRunner(runningAnims, floatProperties, ::onAnimationStep),
                ANIM_POST_DELAY_MS,
            )
        } else {
            animating = false
        }
    }

    companion object {
        private const val ANIM_POST_DELAY_MS = 15L
    }
}

internal class AnimationRunner(
    private val runningAnims: HashMap<Int, MyAnimator>,
    private val floatProperties: FloatArray,
    private val onStep: (Boolean) -> Unit,
) : Runnable {
    override fun run() {
        var hasActive = false
        for (i in propertiesRange) {
            hasActive = processAnimation(i) || hasActive
        }
        onStep(hasActive)
    }

    private fun processAnimation(i: Int): Boolean {
        val anim = runningAnims[i] ?: return false
        val elapsed =
            (System.currentTimeMillis() - anim.startTime).toFloat() / anim.duration
        when {
            elapsed in ELAPSED_MIN..ELAPSED_MAX -> {
                floatProperties[i] = ((anim.toVal - anim.fromVal) * elapsed) + anim.fromVal
            }
            elapsed > ELAPSED_MAX -> {
                anim.runOnEnd?.run()
                runningAnims.remove(i)
            }
        }
        return true
    }

    companion object {
        private val propertiesRange = 0..6
        private const val ELAPSED_MIN = 0.0f
        private const val ELAPSED_MAX = 1.0f
    }
}
