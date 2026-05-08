package at.priv.graf.zazentimer.views

import android.animation.ValueAnimator
import android.content.Context
import android.content.SharedPreferences
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import at.priv.graf.zazentimer.R
import at.priv.graf.zazentimer.ZazenTimerActivity
import java.util.HashMap
import java.util.Locale

class TimerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.timerViewStyle,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        const val PROP_COUNT = 7
        const val PROP_CURRENT_START_SECONDS = 2
        const val PROP_NAME_SWITCH = 6
        const val PROP_NEXT_END_SECONDS = 4
        const val PROP_NEXT_START_SECONDS = 3
        const val PROP_PREV_START_SECONDS = 1
        const val PROP_SESSION_ELAPSED_SECONDS = 5
        const val PROP_SESSION_TOTAL_SECONDS = 0
        const val TIME_SECTION_ELAPSED = 0
        const val TIME_SECTION_MAX = 3
        const val TIME_SECTION_MIN = 0
        const val TIME_SECTION_REMAINING = 1
        const val TIME_SESSION_ELAPSED = 2
        const val TIME_SESSION_REMAINING = 3
        private const val TAG = "ZMT_TimerView"
    }

    private var angleEnd = 0
    private var angleStart = 0
    private var animDuration = 0
    private var animating = false
    private var colorMarkerLine = 0
    private var colorMarkerTriangle = 0
    private var colorRingCurrent = 0
    private var colorRingNext = 0
    private var colorRingPrev = 0
    private var colorRingRemaining = 0
    private var curSectionAlpha = 0f
    private var curSectionSize = 0f
    private var curSectionX = 0f
    private var curSectionXEnd = 0f
    private var curSectionXStart = 0f
    private var curSectionY = 0f
    private var curSectionYEnd = 0f
    private var curSectionYStart = 0f
    private var curTimeX = 0f
    private var curTimeY = 0f
    private var currentSectionName: String = ""
    private var deltaX = 0
    private var deltaY = 0
    private var fixedCurrentStartSeconds = 0
    private var fixedNextStartSeconds = 0
    private var fixedSessionTotalSeconds = 0
    private var floatProperties = FloatArray(7)
    private var interp = AccelerateDecelerateInterpolator()
    private var markerWidth = 0
    private var newNextSectionAlpha = 0f
    private var newNextSectionName: String = ""
    private var newNextSectionX = 0f
    private var newNextSectionXEnd = 0f
    private var newNextSectionXStart = 0f
    private var newNextSectionY = 0f
    private var newNextSectionYEnd = 0f
    private var newNextSectionYStart = 0f
    private var nextSectionColor = 0
    private var nextSectionColorEnd = 0
    private var nextSectionColorStart = 0
    private var nextSectionName: String = ""
    private var nextSectionSize = 0f
    private var nextSectionSizeEnd = 0f
    private var nextSectionSizeStart = 0f
    private var nextSectionX = 0f
    private var nextSectionXEnd = 0f
    private var nextSectionXStart = 0f
    private var nextSectionY = 0f
    private var nextSectionYEnd = 0f
    private var nextSectionYStart = 0f
    private var paddingBottom = 40
    private var paddingLeft = 40
    private var paddingRight = 40
    private var paddingTop = 40
    private var paintCurSection = Paint()
    private var paintCurrent = Paint()
    private var paintGrayLine = Paint()
    private var paintMarkerLine = Paint()
    private var paintNewNextSection = Paint()
    private var paintNext = Paint()
    private var paintNextSection = Paint()
    private var paintPrev = Paint()
    private var paintRemaining = Paint()
    private var paintTimeInfoText = Paint()
    private var paintTimeSectionElapsed = Paint()
    private var paintTimeSectionRemaining = Paint()
    private var paintTimeSessionElapsed = Paint()
    private var paintTimeSessionRemaining = Paint()
    private var paintTriangle = Paint()
    private var prefs: SharedPreferences? = null
    private var rectFull = RectF()
    private var ringWidthPercent = 0.08f
    private var runningAnims = HashMap<Int, MyAnimator>()
    private var sectionElapsedSeconds = 0
    private var showTimeMode = 0
    private var size = 0
    private var timeInfoSize = 0f
    private var timeInfoX = 0f
    private var timeInfoY = 0f
    private var timeTextAnimator = ValueAnimator()
    private var triangle = Path()

    private fun morphColor(i: Int, i2: Int, f: Float): Int {
        val a1 = (i shr 24) and 255
        val r1 = (i shr 16) and 255
        val g1 = (i shr 8) and 255
        val b1 = i and 255

        val a2 = (i2 shr 24) and 255
        val r2 = (i2 shr 16) and 255
        val g2 = (i2 shr 8) and 255
        val b2 = i2 and 255

        val a = a1 + ((a2 - a1) * f).toInt()
        val r = r1 + ((r2 - r1) * f).toInt()
        val g = g1 + ((g2 - g1) * f).toInt()
        val b = b1 + ((b2 - b1) * f).toInt()

        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    init {
        this.paddingBottom = 40
        this.paddingLeft = 40
        this.paddingRight = 40
        this.paddingTop = 40
        this.markerWidth = 6
        this.ringWidthPercent = 0.08f
        this.colorMarkerLine = -12566464
        this.colorMarkerTriangle = -12566464
        this.colorRingPrev = -3282705
        this.colorRingCurrent = -12746077
        this.colorRingNext = -6499103
        this.colorRingRemaining = -986896
        this.angleStart = 120
        this.angleEnd = 420
        this.animDuration = ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION

        val obtainStyledAttributes = context.obtainStyledAttributes(attrs, R.styleable.TimerView, defStyleAttr, defStyleRes)
        this.colorRingPrev = obtainStyledAttributes.getColor(4, -3282705)
        this.colorRingCurrent = obtainStyledAttributes.getColor(2, -12746077)
        this.colorRingNext = obtainStyledAttributes.getColor(3, -6499103)
        this.colorRingRemaining = obtainStyledAttributes.getColor(5, -986896)
        this.colorMarkerLine = obtainStyledAttributes.getColor(0, -12566464)
        this.colorMarkerTriangle = obtainStyledAttributes.getColor(1, -12566464)
        this.paintGrayLine.strokeWidth = 2.0f
        this.paintGrayLine.style = Paint.Style.STROKE
        this.paintGrayLine.color = -12566464
        this.paintTimeSectionElapsed.isAntiAlias = true
        this.paintTimeSectionElapsed.textAlign = Paint.Align.CENTER
        this.paintTimeSectionElapsed.color = colorRingCurrent
        this.paintTimeSectionElapsed.alpha = 255
        this.paintTimeSectionRemaining.isAntiAlias = true
        this.paintTimeSectionRemaining.textAlign = Paint.Align.CENTER
        this.paintTimeSectionRemaining.color = colorRingCurrent
        this.paintTimeSectionRemaining.alpha = 255
        this.paintTimeSessionElapsed.isAntiAlias = true
        this.paintTimeSessionElapsed.textAlign = Paint.Align.CENTER
        this.paintTimeSessionElapsed.color = colorRingCurrent
        this.paintTimeSessionElapsed.alpha = 255
        this.paintTimeSessionRemaining.isAntiAlias = true
        this.paintTimeSessionRemaining.textAlign = Paint.Align.CENTER
        this.paintTimeSessionRemaining.color = colorRingCurrent
        this.paintTimeSessionRemaining.alpha = 255
        this.paintTimeInfoText.isAntiAlias = true
        this.paintTimeInfoText.textAlign = Paint.Align.CENTER
        this.paintTimeInfoText.color = colorRingCurrent
        this.paintTimeInfoText.alpha = 255
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context)
        if (this.prefs!!.contains(ZazenTimerActivity.PREF_KEY_SHOW_ELAPSED_TIME)) {
            if (this.prefs!!.getBoolean(ZazenTimerActivity.PREF_KEY_SHOW_ELAPSED_TIME, true)) {
                this.prefs!!.edit().putInt(ZazenTimerActivity.PREF_KEY_SHOW_TIME_MODE, 0).apply()
            } else {
                this.prefs!!.edit().putInt(ZazenTimerActivity.PREF_KEY_SHOW_TIME_MODE, 1).apply()
            }
            this.prefs!!.edit().remove(ZazenTimerActivity.PREF_KEY_SHOW_ELAPSED_TIME).apply()
        }
        this.showTimeMode = this.prefs!!.getInt(ZazenTimerActivity.PREF_KEY_SHOW_TIME_MODE, 0)
        this.timeTextAnimator = ValueAnimator()
        startTimeInfoFadeOut(7000)
        setOnClickListener {
            showTimeMode++
            if (showTimeMode > 3) {
                showTimeMode = 0
            }
            prefs!!.edit().putInt(ZazenTimerActivity.PREF_KEY_SHOW_TIME_MODE, showTimeMode).apply()
            startTimeInfoFadeOut(3000)
        }
    }

    fun startTimeInfoFadeOut(i: Int) {
        if (timeTextAnimator.isRunning) {
            timeTextAnimator.end()
        }
        timeTextAnimator.setDuration(i.toLong())
        timeTextAnimator.setFloatValues(255.0f, 0.0f)
        timeTextAnimator.start()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Log.d(TAG, "resize: w=" + w + " h=" + h)
        if (w >= h) {
            this.deltaX = (w - h) / 2
            this.deltaY = 0
            this.size = h
        } else {
            this.deltaX = 0
            this.deltaY = (h - w) / 2
            this.size = w
        }
        val f = w / 2.0f
        this.curSectionX = f
        val f2 = h / 2.0f
        this.curSectionY = f2
        this.nextSectionX = f
        this.nextSectionY = (this.size * 0.15f) + f2
        this.curSectionXStart = this.curSectionX
        this.curSectionYStart = this.curSectionY
        this.curSectionXEnd = f
        this.curSectionYEnd = f2 - (this.size * 0.25f)
        this.curSectionAlpha = 1.0f
        this.nextSectionXStart = this.nextSectionX
        this.nextSectionYStart = this.nextSectionY
        this.nextSectionXEnd = this.curSectionXStart
        this.nextSectionYEnd = this.curSectionYStart
        this.timeInfoSize = this.size * 0.05f
        this.curSectionSize = this.size * 0.1f
        this.nextSectionSize = this.size * 0.07f
        this.nextSectionSizeStart = this.nextSectionSize
        this.nextSectionSizeEnd = this.curSectionSize
        this.nextSectionColorStart = colorRingNext
        this.nextSectionColorEnd = colorRingCurrent
        this.newNextSectionXStart = this.nextSectionX
        this.newNextSectionYStart = this.nextSectionY + (this.size * 0.25f)
        this.newNextSectionXEnd = this.nextSectionX
        this.newNextSectionYEnd = this.nextSectionY
        this.newNextSectionAlpha = 0.0f
        this.newNextSectionX = this.newNextSectionXStart
        this.newNextSectionY = this.newNextSectionYStart
        this.curTimeX = f
        this.curTimeY = f2 - this.curSectionSize
        this.timeInfoX = f
        this.timeInfoY = this.curTimeY - this.curSectionSize
    }

    override fun onDraw(canvas: Canvas) {
        val width = getWidth()
        val height = getHeight()
        val i = (ringWidthPercent * size).toInt()
        val f = i.toFloat()
        val i2 = (f * 1.0f).toInt()
        val i3 = i2 / 2
        paintTimeSectionElapsed.textSize = curSectionSize
        paintTimeSectionRemaining.textSize = curSectionSize
        paintTimeSessionElapsed.textSize = curSectionSize
        paintTimeSessionRemaining.textSize = curSectionSize
        paintTimeInfoText.textSize = timeInfoSize
        paintMarkerLine.style = Paint.Style.STROKE
        paintMarkerLine.isAntiAlias = true
        paintMarkerLine.strokeWidth = markerWidth.toFloat()
        paintMarkerLine.color = colorMarkerLine
        paintTriangle.style = Paint.Style.FILL
        paintTriangle.isAntiAlias = true
        paintTriangle.color = colorMarkerTriangle
        paintPrev.strokeWidth = f
        paintPrev.isAntiAlias = true
        paintPrev.style = Paint.Style.STROKE
        paintPrev.color = colorRingPrev
        paintCurrent.strokeWidth = f
        paintCurrent.isAntiAlias = true
        paintCurrent.style = Paint.Style.STROKE
        paintCurrent.color = colorRingCurrent
        paintNext.strokeWidth = f
        paintNext.isAntiAlias = true
        paintNext.style = Paint.Style.STROKE
        paintNext.color = colorRingNext
        paintRemaining.strokeWidth = f
        paintRemaining.isAntiAlias = true
        paintRemaining.style = Paint.Style.STROKE
        paintRemaining.color = colorRingRemaining
        val i4 = i / 2
        rectFull.set(
            (paddingLeft + i4 + deltaX).toFloat(),
            (paddingTop + i4 + deltaY).toFloat(),
            (width - (paddingRight + i4) - deltaX).toFloat(),
            (height - (paddingBottom + i4) - deltaY).toFloat()
        )
        val f2 = angleStart.toFloat()
        canvas.drawArc(rectFull, f2, (angleStart + ((angleEnd - angleStart) * getPrevStartSeconds() / getSessionTotalSeconds()).toInt()) - f2, false, paintRemaining)
        val prevStartSeconds = angleStart + ((angleEnd - angleStart) * getPrevStartSeconds() / getSessionTotalSeconds()).toInt()
        canvas.drawArc(rectFull, prevStartSeconds.toFloat(), (angleStart + ((angleEnd - angleStart) * getCurrentStartSeconds() / getSessionTotalSeconds()).toInt()) - prevStartSeconds.toFloat(), false, paintPrev)
        val currentStartSeconds = angleStart + ((angleEnd - angleStart) * getCurrentStartSeconds() / getSessionTotalSeconds()).toInt()
        canvas.drawArc(rectFull, currentStartSeconds.toFloat(), (angleStart + ((angleEnd - angleStart) * getNextStartSeconds() / getSessionTotalSeconds()).toInt()) - currentStartSeconds.toFloat(), false, paintCurrent)
        val nextStartSeconds = angleStart + ((angleEnd - angleStart) * getNextStartSeconds() / getSessionTotalSeconds()).toInt()
        canvas.drawArc(rectFull, nextStartSeconds.toFloat(), (angleStart + ((angleEnd - angleStart) * getNextEndSeconds() / getSessionTotalSeconds()).toInt()) - nextStartSeconds.toFloat(), false, paintNext)
        val nextEndSeconds = angleStart + ((angleEnd - angleStart) * getNextEndSeconds() / getSessionTotalSeconds()).toInt()
        canvas.drawArc(rectFull, nextEndSeconds.toFloat(), (angleEnd - nextEndSeconds).toFloat(), false, paintRemaining)
        val sessionElapsedSeconds = ((angleStart + ((angleEnd - angleStart) * getSessionElapsedSeconds() / getSessionTotalSeconds())) * 3.1415 / 180.0).toDouble()
        val cos = Math.cos(sessionElapsedSeconds).toFloat()
        val sin = Math.sin(sessionElapsedSeconds).toFloat()
        val width2 = (rectFull.width() * cos / 2.0f) + rectFull.centerX()
        val height2 = (rectFull.height() * sin / 2.0f) + rectFull.centerY()
        val f3 = i2.toFloat()
        val f4 = (cos * f3) / 2.0f
        val f5 = width2 + f4
        val f6 = (f3 * sin) / 2.0f
        val f7 = height2 + f6
        val f8 = width2 - f4
        val f9 = height2 - f6
        val f10 = i3.toFloat()
        val f11 = cos * f10
        val f12 = f5 + f11
        val f13 = sin * f10
        val f14 = f12 - f13
        val f15 = f7 + f13
        val f16 = f15 + f11
        val f17 = f12 + f13
        val f18 = f15 - f11
        val f19 = f8 - f11
        val f20 = f19 - f13
        val f21 = f9 - f13
        val f22 = f21 + f11
        val f23 = f19 + f13
        val f24 = f21 - f11
        val f25 = f11 * 0.5f
        val f26 = f13 * 0.5f
        canvas.drawLine(f5 + f25, f7 + f26, f8 - f25, f9 - f26, paintMarkerLine)
        triangle.reset()
        triangle.moveTo(f5, f7)
        triangle.lineTo(f14, f16)
        triangle.lineTo(f17, f18)
        triangle.close()
        canvas.drawPath(triangle, paintTriangle)
        triangle.reset()
        triangle.moveTo(f8, f9)
        triangle.lineTo(f20, f22)
        triangle.lineTo(f23, f24)
        triangle.close()
        canvas.drawPath(triangle, paintTriangle)
        val interpolation = interp.getInterpolation(floatProperties[6])
        this.curSectionX = curSectionXStart + ((curSectionXEnd - curSectionXStart) * interpolation)
        this.curSectionY = curSectionYStart + ((curSectionYEnd - curSectionYStart) * interpolation)
        this.nextSectionX = nextSectionXStart + ((nextSectionXEnd - nextSectionXStart) * interpolation)
        this.nextSectionY = nextSectionYStart + ((nextSectionYEnd - nextSectionYStart) * interpolation)
        this.curSectionAlpha = 1.0f - interpolation
        this.nextSectionSize = nextSectionSizeStart + ((nextSectionSizeEnd - nextSectionSizeStart) * interpolation)
        this.newNextSectionX = newNextSectionXStart + ((newNextSectionXEnd - newNextSectionXStart) * interpolation)
        this.newNextSectionY = newNextSectionYStart + ((newNextSectionYEnd - newNextSectionYStart) * interpolation)
        this.newNextSectionAlpha = interpolation
        this.nextSectionColor = morphColor(nextSectionColorStart, nextSectionColorEnd, interpolation)
        paintCurSection.isAntiAlias = true
        paintCurSection.textAlign = Paint.Align.CENTER
        paintCurSection.textSize = curSectionSize
        paintCurSection.color = colorRingCurrent
        paintCurSection.alpha = (curSectionAlpha * 255.0f).toInt()
        paintNewNextSection.isAntiAlias = true
        paintNewNextSection.textAlign = Paint.Align.CENTER
        paintNewNextSection.textSize = nextSectionSizeStart
        paintNewNextSection.color = colorRingNext
        paintNewNextSection.alpha = (newNextSectionAlpha * 255.0f).toInt()
        paintNextSection.isAntiAlias = true
        paintNextSection.textAlign = Paint.Align.CENTER
        paintNextSection.textSize = nextSectionSize
        paintNextSection.color = nextSectionColor
        paintNextSection.alpha = 255
        if (isInEditMode) {
            canvas.drawText("05:20", curTimeX, curTimeY, paintCurSection)
            canvas.drawText("current sec", curSectionX, curSectionY, paintCurSection)
            canvas.drawText("next sec", nextSectionX, nextSectionY, paintNextSection)
            canvas.drawText("next next sec", newNextSectionX, newNextSectionY, paintNewNextSection)
            return
        }
        val sectionElapsedSeconds = getSectionElapsedSeconds()
        var str = ""
        var paint: Paint? = null
        var str2 = ""
        if (showTimeMode == 0) {
            str = String.format(Locale.getDefault(), "%1$02d:%2$02d", Integer.valueOf(sectionElapsedSeconds / 60), Integer.valueOf(sectionElapsedSeconds % 60))
            paint = paintTimeSectionElapsed
            str2 = context.getString(R.string.time_section_elapsed)
        } else if (showTimeMode == 1) {
            val i5 = (fixedNextStartSeconds - fixedCurrentStartSeconds) - sectionElapsedSeconds
            str = String.format(Locale.getDefault(), "-%1$02d:%2$02d", Integer.valueOf(i5 / 60), Integer.valueOf(i5 % 60))
            paint = paintTimeSectionRemaining
            str2 = context.getString(R.string.time_section_remaining)
        } else if (showTimeMode == 2) {
            val i6 = fixedCurrentStartSeconds + sectionElapsedSeconds
            str = String.format(Locale.getDefault(), "%1$02d:%2$02d", Integer.valueOf(i6 / 60), Integer.valueOf(i6 % 60))
            paint = paintTimeSessionElapsed
            str2 = context.getString(R.string.time_session_elapsed)
        } else if (showTimeMode == 3) {
            val i7 = fixedSessionTotalSeconds - (fixedCurrentStartSeconds + sectionElapsedSeconds)
            str = String.format(Locale.getDefault(), "-%1$02d:%2$02d", Integer.valueOf(i7 / 60), Integer.valueOf(i7 % 60))
            paint = paintTimeSessionRemaining
            str2 = context.getString(R.string.time_session_remaining)
        }
        if (timeTextAnimator.isRunning) {
            paintTimeInfoText.alpha = ((timeTextAnimator.animatedValue as Float).toInt())
        }
        canvas.drawText(str2, timeInfoX, timeInfoY, paintTimeInfoText)
        canvas.drawText(str, curTimeX, curTimeY, paint!!)
        canvas.drawText(currentSectionName, curSectionX, curSectionY, paintCurSection)
        canvas.drawText(nextSectionName, nextSectionX, nextSectionY, paintNextSection)
        canvas.drawText(newNextSectionName, newNextSectionX, newNextSectionY, paintNewNextSection)
    }

    private fun startAnimation() {
        if (this.animating) {
            return
        }
        this.animating = true
        postDelayed(ChangeAnimation(), 15L)
    }

    inner class ChangeAnimation : Runnable {
        override fun run() {
            var z = false
            for (i in 0..6) {
                val myAnimator = runningAnims[i]
                if (myAnimator != null) {
                    val currentTimeMillis = (System.currentTimeMillis() - myAnimator.startTime).toFloat() / myAnimator.duration
                    if (currentTimeMillis >= 0.0f && currentTimeMillis <= 1.0f) {
                        floatProperties[i] = ((myAnimator.toVal - myAnimator.fromVal) * currentTimeMillis) + myAnimator.fromVal
                    } else if (currentTimeMillis > 1.0f) {
                        val runOnEnd = myAnimator.runOnEnd
                        if (runOnEnd == null) {
                            floatProperties[i] = myAnimator.toVal
                        } else {
                            runOnEnd.run()
                        }
                        runningAnims.remove(i)
                    }
                    z = true
                }
            }
            postInvalidate()
            if (!z) {
                animating = false
            } else {
                postDelayed(this, 15L)
            }
        }
    }

    class MyAnimator {
        var duration = 0
        var fromVal = 0f
        var propertyIdx = 0
        var runOnEnd: Runnable? = null
        var startTime: Long = 0L
        var toVal = 0f
    }

    fun animateTo(i: Int, i2: Int, i3: Int) {
        animateTo(i, i2, i3, null)
    }

    fun animateTo(i: Int, i2: Int, i3: Int, runnable: Runnable?) {
        var myAnimator = runningAnims[i]
        if (myAnimator == null) {
            myAnimator = MyAnimator()
        }
        myAnimator.runOnEnd = runnable
        myAnimator.propertyIdx = i
        myAnimator.duration = i3
        myAnimator.toVal = i2.toFloat()
        myAnimator.fromVal = floatProperties[i]
        myAnimator.startTime = System.currentTimeMillis()
        runningAnims[i] = myAnimator
        if (animating) {
            return
        }
        startAnimation()
    }

    fun setNumTotalSeconds(i: Int) {
        fixedSessionTotalSeconds = i
        animateTo(0, i, animDuration)
    }

    fun setPrevStartSeconds(i: Int) {
        animateTo(1, i, animDuration)
    }

    fun setCurrentStartSeconds(i: Int) {
        fixedCurrentStartSeconds = i
        animateTo(2, i, animDuration)
    }

    fun setNextStartSeconds(i: Int) {
        fixedNextStartSeconds = i
        animateTo(3, i, animDuration)
    }

    fun setNextEndSeconds(i: Int) {
        animateTo(4, i, animDuration)
    }

    fun setSessionElapsedSeconds(i: Int) {
        animateTo(5, i, animDuration)
    }

    fun setSectionElapsedSeconds(i: Int) {
        sectionElapsedSeconds = i
    }

    fun setSectionNames(str: String, str2: String, str3: String) {
        if (((if (str == currentSectionName && str2 == nextSectionName) false else true)) && runningAnims[6] == null) {
            newNextSectionName = str2
            animateTo(6, 1, 1400, Runnable {
                floatProperties[6] = 0.0f
                currentSectionName = str
                nextSectionName = str2
            })
        }
    }

    fun setSectionNamesNoAnim(str: String, str2: String) {
        currentSectionName = str
        nextSectionName = str2
    }

    fun getColorMarkerLine(): Int {
        return colorMarkerLine
    }

    fun getColorMarkerTriangle(): Int {
        return colorMarkerTriangle
    }

    fun getColorRingPrev(): Int {
        return colorRingPrev
    }

    fun getColorRingCurrent(): Int {
        return colorRingCurrent
    }

    fun getColorRingNext(): Int {
        return colorRingNext
    }

    fun getColorRingRemaining(): Int {
        return colorRingRemaining
    }

    fun getPrevStartSeconds(): Float {
        return floatProperties[1]
    }

    fun getSessionTotalSeconds(): Float {
        return floatProperties[0]
    }

    fun getCurrentStartSeconds(): Float {
        return floatProperties[2]
    }

    fun getNextStartSeconds(): Float {
        return floatProperties[3]
    }

    fun getNextEndSeconds(): Float {
        return floatProperties[4]
    }

    fun getSessionElapsedSeconds(): Float {
        return floatProperties[5]
    }

    fun getSectionElapsedSeconds(): Int {
        return sectionElapsedSeconds
    }
}
