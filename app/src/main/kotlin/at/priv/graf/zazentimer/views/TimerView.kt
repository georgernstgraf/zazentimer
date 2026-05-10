package at.priv.graf.zazentimer.views

import android.animation.ValueAnimator
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import at.priv.graf.zazentimer.R
import at.priv.graf.zazentimer.ZazenTimerActivity
import java.util.HashMap
import java.util.Locale

@Suppress("TooManyFunctions")
class TimerView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = R.attr.timerViewStyle,
        defStyleRes: Int = 0,
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

            private const val COLOR_COMPONENT_MASK = 255
            private const val ALPHA_SHIFT = 24
            private const val RED_SHIFT = 16
            private const val GREEN_SHIFT = 8

            private const val DEFAULT_PADDING = 40
            private const val DEFAULT_MARKER_WIDTH = 6
            private const val DEFAULT_RING_WIDTH_PERCENT = 0.08f
            private const val GRAY_LINE_STROKE_WIDTH = 2.0f
            private const val ANGLE_START_DEGREES = 120
            private const val ANGLE_END_DEGREES = 420

            private const val DEFAULT_FADE_OUT_MS = 7000
            private const val CLICK_FADE_OUT_MS = 3000
            private const val SECTION_MORPH_DURATION_MS = 1400

            private const val FULL_ALPHA = 255
            private const val ZERO_ALPHA = 0
            private const val FLOAT_ALPHA_RANGE_START = 255.0f
            private const val FLOAT_ALPHA_RANGE_END = 0.0f
            private const val ALPHA_MAX_FLOAT = 1.0f
            private const val ALPHA_MIN_FLOAT = 0.0f
            private const val MORPH_END_VALUE = 0.0f

            private const val PI_APPROXIMATION = 3.1415
            private const val DEGREES_TO_RADIANS_DIVISOR = 180.0
            private const val HALF_DIVISOR = 2
            private const val HALF_DIVISOR_FLOAT = 2.0f
            private const val HALF_FACTOR = 0.5f

            private const val SECONDS_PER_MINUTE = 60
            private const val TIME_FORMAT_POSITIVE = "%1$02d:%2$02d"
            private const val TIME_FORMAT_NEGATIVE = "-%1$02d:%2$02d"

            private const val SHOW_TIME_MODE_MODULUS = 4
            private const val SHOW_TIME_MODE_DEFAULT = 0
            private const val SHOW_TIME_MODE_SECTION_ELAPSED = 0
            private const val SHOW_TIME_MODE_SECTION_REMAINING = 1
            private const val SHOW_TIME_MODE_SESSION_ELAPSED = 2
            private const val SHOW_TIME_MODE_SESSION_REMAINING = 3

            private const val CUR_SECTION_SIZE_FACTOR = 0.1f
            private const val NEXT_SECTION_SIZE_FACTOR = 0.07f
            private const val TIME_INFO_SIZE_FACTOR = 0.05f
            private const val NEXT_SECTION_Y_FACTOR = 0.15f
            private const val CUR_SECTION_Y_END_FACTOR = 0.25f
            private const val NEW_NEXT_Y_OFFSET_FACTOR = 0.25f

            private const val STYLEABLE_IDX_MARKER_LINE = 0
            private const val STYLEABLE_IDX_MARKER_TRIANGLE = 1
            private const val STYLEABLE_IDX_RING_CURRENT = 2
            private const val STYLEABLE_IDX_RING_NEXT = 3
            private const val STYLEABLE_IDX_RING_PREV = 4
            private const val STYLEABLE_IDX_RING_REMAINING = 5

            private const val COLOR_DEFAULT_MARKER_LINE = -12566464
            private const val COLOR_DEFAULT_MARKER_TRIANGLE = -12566464
            private const val COLOR_DEFAULT_RING_CURRENT = -12746077
            private const val COLOR_DEFAULT_RING_NEXT = -6499103
            private const val COLOR_DEFAULT_RING_PREV = -3282705
            private const val COLOR_DEFAULT_RING_REMAINING = -986896

            private const val EDIT_MODE_TIME_TEXT = "05:20"
            private const val EDIT_MODE_CURRENT_LABEL = "current sec"
            private const val EDIT_MODE_NEXT_LABEL = "next sec"
            private const val EDIT_MODE_NEXT_NEXT_LABEL = "next next sec"

            private const val ONE_INT = 1

            private fun morphColor(
                c1: Int,
                c2: Int,
                frac: Float,
            ): Int {
                val a1 = (c1 shr ALPHA_SHIFT) and COLOR_COMPONENT_MASK
                val r1 = (c1 shr RED_SHIFT) and COLOR_COMPONENT_MASK
                val g1 = (c1 shr GREEN_SHIFT) and COLOR_COMPONENT_MASK
                val b1 = c1 and COLOR_COMPONENT_MASK
                val a2 = (c2 shr ALPHA_SHIFT) and COLOR_COMPONENT_MASK
                val r2 = (c2 shr RED_SHIFT) and COLOR_COMPONENT_MASK
                val g2 = (c2 shr GREEN_SHIFT) and COLOR_COMPONENT_MASK
                val b2 = c2 and COLOR_COMPONENT_MASK
                val a = a1 + ((a2 - a1) * frac).toInt()
                val r = r1 + ((r2 - r1) * frac).toInt()
                val g = g1 + ((g2 - g1) * frac).toInt()
                val b = b1 + ((b2 - b1) * frac).toInt()
                return (a shl ALPHA_SHIFT) or (r shl RED_SHIFT) or (g shl GREEN_SHIFT) or b
            }

            @Suppress("LongParameterList")
            private fun drawRingSegments(
                canvas: Canvas,
                rectFull: RectF,
                angleStart: Int,
                angleEnd: Int,
                fp: FloatArray,
                paintRemaining: Paint,
                paintPrev: Paint,
                paintCurrent: Paint,
                paintNext: Paint,
            ) {
                val totalSecs = fp[PROP_SESSION_TOTAL_SECONDS]
                val prevSecs = fp[PROP_PREV_START_SECONDS]
                val curSecs = fp[PROP_CURRENT_START_SECONDS]
                val nextSecs = fp[PROP_NEXT_START_SECONDS]
                val nextEndSecs = fp[PROP_NEXT_END_SECONDS]
                val startFloat = angleStart.toFloat()
                val fullRange = angleEnd - angleStart

                fun angleAt(secs: Float): Int = angleStart + ((fullRange * secs) / totalSecs).toInt()

                val s1 = angleAt(prevSecs) - angleStart
                canvas.drawArc(rectFull, startFloat, s1.toFloat(), false, paintRemaining)
                val s2 = angleAt(curSecs) - angleAt(prevSecs)
                canvas.drawArc(rectFull, angleAt(prevSecs).toFloat(), s2.toFloat(), false, paintPrev)
                val s3 = angleAt(nextSecs) - angleAt(curSecs)
                canvas.drawArc(rectFull, angleAt(curSecs).toFloat(), s3.toFloat(), false, paintCurrent)
                val s4 = angleAt(nextEndSecs) - angleAt(nextSecs)
                canvas.drawArc(rectFull, angleAt(nextSecs).toFloat(), s4.toFloat(), false, paintNext)
                val s5 = angleEnd - angleAt(nextEndSecs)
                canvas.drawArc(rectFull, angleAt(nextEndSecs).toFloat(), s5.toFloat(), false, paintRemaining)
            }

            @Suppress("LongParameterList")
            private fun drawMarker(
                canvas: Canvas,
                rectFull: RectF,
                angleStart: Int,
                angleEnd: Int,
                fp: FloatArray,
                ringPx: Int,
                halfPx: Int,
                paintMarkerLine: Paint,
                paintTriangle: Paint,
                triangle: Path,
            ) {
                val sessionElapsed = fp[PROP_SESSION_ELAPSED_SECONDS]
                val totalSecs = fp[PROP_SESSION_TOTAL_SECONDS]
                val fullRange = angleEnd - angleStart
                val angle = angleStart + ((fullRange * sessionElapsed) / totalSecs).toInt()
                val rad = (angle * PI_APPROXIMATION / DEGREES_TO_RADIANS_DIVISOR).toDouble()
                val cos = Math.cos(rad).toFloat()
                val sin = Math.sin(rad).toFloat()
                val cx = rectFull.centerX()
                val cy = rectFull.centerY()
                val rx = rectFull.width() * cos / HALF_DIVISOR_FLOAT
                val ry = rectFull.height() * sin / HALF_DIVISOR_FLOAT
                val lx = rx + cx
                val ly = ry + cy
                val rf = ringPx.toFloat()
                val hf = halfPx.toFloat()
                val cosE = (cos * rf) / HALF_DIVISOR_FLOAT
                val sinE = (sin * rf) / HALF_DIVISOR_FLOAT
                val cosH = cos * hf
                val sinH = sin * hf
                val x1 = lx + cosE
                val y1 = ly + sinE
                val x2 = lx - cosE
                val y2 = ly - sinE
                val tx1 = x1 + cosH
                val ty1 = y1 + sinH
                val p1x = tx1 - sinH
                val p1y = ty1 + cosH
                val p2x = tx1 + sinH
                val p2y = ty1 - cosH
                val tx2 = x2 - cosH
                val ty2 = y2 - sinH
                val p3x = tx2 - sinH
                val p3y = ty2 + cosH
                val p4x = tx2 + sinH
                val p4y = ty2 - cosH
                val offX = cosH * HALF_FACTOR
                val offY = sinH * HALF_FACTOR
                canvas.drawLine(x1 + offX, y1 + offY, x2 - offX, y2 - offY, paintMarkerLine)
                triangle.reset()
                triangle.moveTo(x1, y1)
                triangle.lineTo(p1x, p1y)
                triangle.lineTo(p2x, p2y)
                triangle.close()
                canvas.drawPath(triangle, paintTriangle)
                triangle.reset()
                triangle.moveTo(x2, y2)
                triangle.lineTo(p3x, p3y)
                triangle.lineTo(p4x, p4y)
                triangle.close()
                canvas.drawPath(triangle, paintTriangle)
            }

            @Suppress("LongParameterList")
            private fun drawSectionLabels(
                canvas: Canvas,
                fp: FloatArray,
                interp: AccelerateDecelerateInterpolator,
                cxs: Float,
                cxe: Float,
                cys: Float,
                cye: Float,
                csSize: Float,
                nxs: Float,
                nxe: Float,
                nys: Float,
                nye: Float,
                nsStart: Float,
                nsEnd: Float,
                ncStart: Int,
                ncEnd: Int,
                nnxs: Float,
                nnxe: Float,
                nnys: Float,
                nnye: Float,
                colorRingCurrent: Int,
                colorRingNext: Int,
                paintCur: Paint,
                paintNextSec: Paint,
                paintNewNext: Paint,
                curName: String,
                nextName: String,
                newNextName: String,
            ) {
                val t = interp.getInterpolation(fp[PROP_NAME_SWITCH])
                val dcx = cxs + ((cxe - cxs) * t)
                val dcy = cys + ((cye - cys) * t)
                val dnx = nxs + ((nxe - nxs) * t)
                val dny = nys + ((nye - nys) * t)
                val drawAlpha = ALPHA_MAX_FLOAT - t
                val dns = nsStart + ((nsEnd - nsStart) * t)
                val dnnx = nnxs + ((nnxe - nnxs) * t)
                val dnny = nnys + ((nnye - nnys) * t)
                val dnnAlpha = t
                val dnColor = morphColor(ncStart, ncEnd, t)
                paintCur.apply {
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                    textSize = csSize
                    color = colorRingCurrent
                    alpha = (drawAlpha * FLOAT_ALPHA_RANGE_START).toInt()
                }
                paintNewNext.apply {
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                    textSize = nsStart
                    color = colorRingNext
                    alpha = (dnnAlpha * FLOAT_ALPHA_RANGE_START).toInt()
                }
                paintNextSec.apply {
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                    textSize = dns
                    color = dnColor
                    alpha = FULL_ALPHA
                }
                canvas.drawText(curName, dcx, dcy, paintCur)
                canvas.drawText(nextName, dnx, dny, paintNextSec)
                canvas.drawText(newNextName, dnnx, dnny, paintNewNext)
            }

            @Suppress("LongParameterList")
            private fun drawTimeText(
                canvas: Canvas,
                ctx: Float,
                cty: Float,
                csz: Float,
                tix: Float,
                tiy: Float,
                tisz: Float,
                showTimeMode: Int,
                fcs: Int,
                fns: Int,
                fst: Int,
                secElapsed: Int,
                tta: ValueAnimator,
                paintInfo: Paint,
                paintE: Paint,
                paintR: Paint,
                paintSE: Paint,
                paintSR: Paint,
                context: Context,
            ) {
                paintE.textSize = csz
                paintR.textSize = csz
                paintSE.textSize = csz
                paintSR.textSize = csz
                paintInfo.textSize = tisz
                val label: String
                val dispTime: String
                val paint: Paint
                when (showTimeMode) {
                    SHOW_TIME_MODE_SECTION_ELAPSED -> {
                        dispTime = formatTimeValue(secElapsed)
                        paint = paintE
                        label = context.getString(R.string.time_section_elapsed)
                    }
                    SHOW_TIME_MODE_SECTION_REMAINING -> {
                        val secs = (fns - fcs) - secElapsed
                        dispTime = formatTimeValueNegative(secs)
                        paint = paintR
                        label = context.getString(R.string.time_section_remaining)
                    }
                    SHOW_TIME_MODE_SESSION_ELAPSED -> {
                        val secs = fcs + secElapsed
                        dispTime = formatTimeValue(secs)
                        paint = paintSE
                        label = context.getString(R.string.time_session_elapsed)
                    }
                    SHOW_TIME_MODE_SESSION_REMAINING -> {
                        val secs = fst - (fcs + secElapsed)
                        dispTime = formatTimeValueNegative(secs)
                        paint = paintSR
                        label = context.getString(R.string.time_session_remaining)
                    }
                    else -> return
                }
                if (tta.isRunning) {
                    paintInfo.alpha = (tta.animatedValue as Float).toInt()
                }
                canvas.drawText(label, tix, tiy, paintInfo)
                canvas.drawText(dispTime, ctx, cty, paint)
            }

            private fun formatTimeValue(totSecs: Int): String {
                val m = totSecs / SECONDS_PER_MINUTE
                val s = totSecs % SECONDS_PER_MINUTE
                return String.format(Locale.getDefault(), TIME_FORMAT_POSITIVE, m, s)
            }

            private fun formatTimeValueNegative(totSecs: Int): String {
                val m = totSecs / SECONDS_PER_MINUTE
                val s = totSecs % SECONDS_PER_MINUTE
                return String.format(Locale.getDefault(), TIME_FORMAT_NEGATIVE, m, s)
            }

            @Suppress("LongParameterList")
            private fun setupPaintAndRect(
                rwp: Float,
                size: Int,
                mw: Int,
                cml: Int,
                cmt: Int,
                crp: Int,
                crc: Int,
                crn: Int,
                crr: Int,
                pl: Int,
                pt: Int,
                pr: Int,
                pb: Int,
                dx: Int,
                dy: Int,
                w: Int,
                h: Int,
                paintPrev: Paint,
                paintCur: Paint,
                paintNext: Paint,
                paintRem: Paint,
                paintML: Paint,
                paintTri: Paint,
                rect: RectF,
            ) {
                val rwPx = (rwp * size).toInt()
                val rwF = rwPx.toFloat()
                paintML.apply {
                    style = Paint.Style.STROKE
                    isAntiAlias = true
                    strokeWidth = mw.toFloat()
                    color = cml
                }
                paintTri.apply {
                    style = Paint.Style.FILL
                    isAntiAlias = true
                    color = cmt
                }
                for (p in arrayOf(paintPrev, paintCur, paintNext, paintRem)) {
                    p.apply {
                        strokeWidth = rwF
                        isAntiAlias = true
                        style = Paint.Style.STROKE
                    }
                }
                paintPrev.color = crp
                paintCur.color = crc
                paintNext.color = crn
                paintRem.color = crr
                val inset = rwPx / HALF_DIVISOR
                rect.set(
                    (pl + inset + dx).toFloat(),
                    (pt + inset + dy).toFloat(),
                    (w - (pr + inset) - dx).toFloat(),
                    (h - (pb + inset) - dy).toFloat(),
                )
            }
        }

        private var angleEnd = ANGLE_END_DEGREES
        private var angleStart = ANGLE_START_DEGREES
        private var animDuration = ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION
        private var colorMarkerLine = COLOR_DEFAULT_MARKER_LINE
        private var colorMarkerTriangle = COLOR_DEFAULT_MARKER_TRIANGLE
        private var colorRingCurrent = COLOR_DEFAULT_RING_CURRENT
        private var colorRingNext = COLOR_DEFAULT_RING_NEXT
        private var colorRingPrev = COLOR_DEFAULT_RING_PREV
        private var colorRingRemaining = COLOR_DEFAULT_RING_REMAINING
        private var curSectionAlpha = ZERO_ALPHA.toFloat()
        private var curSectionSize = ZERO_ALPHA.toFloat()
        private var curSectionX = ZERO_ALPHA.toFloat()
        private var curSectionXEnd = ZERO_ALPHA.toFloat()
        private var curSectionXStart = ZERO_ALPHA.toFloat()
        private var curSectionY = ZERO_ALPHA.toFloat()
        private var curSectionYEnd = ZERO_ALPHA.toFloat()
        private var curSectionYStart = ZERO_ALPHA.toFloat()
        private var curTimeX = ZERO_ALPHA.toFloat()
        private var curTimeY = ZERO_ALPHA.toFloat()
        private var currentSectionName: String = ""
        private var deltaX = ZERO_ALPHA
        private var deltaY = ZERO_ALPHA
        private var fixedCurrentStartSeconds = ZERO_ALPHA
        private var fixedNextStartSeconds = ZERO_ALPHA
        private var fixedSessionTotalSeconds = ZERO_ALPHA
        private var interp = AccelerateDecelerateInterpolator()
        private var markerWidth = DEFAULT_MARKER_WIDTH
        private var newNextSectionAlpha = ZERO_ALPHA.toFloat()
        private var newNextSectionName: String = ""
        private var newNextSectionX = ZERO_ALPHA.toFloat()
        private var newNextSectionXEnd = ZERO_ALPHA.toFloat()
        private var newNextSectionXStart = ZERO_ALPHA.toFloat()
        private var newNextSectionY = ZERO_ALPHA.toFloat()
        private var newNextSectionYEnd = ZERO_ALPHA.toFloat()
        private var newNextSectionYStart = ZERO_ALPHA.toFloat()
        private var nextSectionColorEnd = COLOR_DEFAULT_RING_CURRENT
        private var nextSectionColorStart = COLOR_DEFAULT_RING_NEXT
        private var nextSectionName: String = ""
        private var nextSectionSize = ZERO_ALPHA.toFloat()
        private var nextSectionSizeEnd = ZERO_ALPHA.toFloat()
        private var nextSectionSizeStart = ZERO_ALPHA.toFloat()
        private var nextSectionX = ZERO_ALPHA.toFloat()
        private var nextSectionXEnd = ZERO_ALPHA.toFloat()
        private var nextSectionXStart = ZERO_ALPHA.toFloat()
        private var nextSectionY = ZERO_ALPHA.toFloat()
        private var nextSectionYEnd = ZERO_ALPHA.toFloat()
        private var nextSectionYStart = ZERO_ALPHA.toFloat()
        private var paddingBottom = DEFAULT_PADDING
        private var paddingLeft = DEFAULT_PADDING
        private var paddingRight = DEFAULT_PADDING
        private var paddingTop = DEFAULT_PADDING
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
        private var ringWidthPercent = DEFAULT_RING_WIDTH_PERCENT
        private var sectionElapsedSeconds = ZERO_ALPHA
        private var showTimeMode = SHOW_TIME_MODE_DEFAULT
        private var size = ZERO_ALPHA
        private var timeInfoSize = ZERO_ALPHA.toFloat()
        private var timeInfoX = ZERO_ALPHA.toFloat()
        private var timeInfoY = ZERO_ALPHA.toFloat()
        private var timeTextAnimator = ValueAnimator()
        private var triangle = Path()

        private val animHelper by lazy {
            TimerAnimator(this, HashMap(), FloatArray(PROP_COUNT), animDuration)
        }
        private val floatProperties: FloatArray get() = animHelper.floatProperties
        private val runningAnims: HashMap<Int, MyAnimator> get() = animHelper.runningAnims

        val colorMarkerLineVal: Int get() = colorMarkerLine
        val colorMarkerTriangleVal: Int get() = colorMarkerTriangle
        val colorRingPrevVal: Int get() = colorRingPrev
        val colorRingCurrentVal: Int get() = colorRingCurrent
        val colorRingNextVal: Int get() = colorRingNext
        val colorRingRemainingVal: Int get() = colorRingRemaining

        init {
            paddingBottom = DEFAULT_PADDING
            paddingLeft = DEFAULT_PADDING
            paddingRight = DEFAULT_PADDING
            paddingTop = DEFAULT_PADDING
            markerWidth = DEFAULT_MARKER_WIDTH
            ringWidthPercent = DEFAULT_RING_WIDTH_PERCENT
            colorMarkerLine = COLOR_DEFAULT_MARKER_LINE
            colorMarkerTriangle = COLOR_DEFAULT_MARKER_TRIANGLE
            colorRingPrev = COLOR_DEFAULT_RING_PREV
            colorRingCurrent = COLOR_DEFAULT_RING_CURRENT
            colorRingNext = COLOR_DEFAULT_RING_NEXT
            colorRingRemaining = COLOR_DEFAULT_RING_REMAINING
            angleStart = ANGLE_START_DEGREES
            angleEnd = ANGLE_END_DEGREES
            animDuration = ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION

            val styled =
                context.obtainStyledAttributes(
                    attrs,
                    R.styleable.TimerView,
                    defStyleAttr,
                    defStyleRes,
                )
            colorRingPrev = styled.getColor(STYLEABLE_IDX_RING_PREV, COLOR_DEFAULT_RING_PREV)
            colorRingCurrent = styled.getColor(STYLEABLE_IDX_RING_CURRENT, COLOR_DEFAULT_RING_CURRENT)
            colorRingNext = styled.getColor(STYLEABLE_IDX_RING_NEXT, COLOR_DEFAULT_RING_NEXT)
            colorRingRemaining = styled.getColor(STYLEABLE_IDX_RING_REMAINING, COLOR_DEFAULT_RING_REMAINING)
            colorMarkerLine = styled.getColor(STYLEABLE_IDX_MARKER_LINE, COLOR_DEFAULT_MARKER_LINE)
            colorMarkerTriangle = styled.getColor(STYLEABLE_IDX_MARKER_TRIANGLE, COLOR_DEFAULT_MARKER_TRIANGLE)
            styled.recycle()

            paintGrayLine.strokeWidth = GRAY_LINE_STROKE_WIDTH
            paintGrayLine.style = Paint.Style.STROKE
            paintGrayLine.color = COLOR_DEFAULT_MARKER_LINE

            for (p in arrayOf(
                paintTimeSectionElapsed,
                paintTimeSectionRemaining,
                paintTimeSessionElapsed,
                paintTimeSessionRemaining,
            )) {
                p.isAntiAlias = true
                p.textAlign = Paint.Align.CENTER
                p.color = colorRingCurrent
                p.alpha = FULL_ALPHA
            }
            paintTimeInfoText.isAntiAlias = true
            paintTimeInfoText.textAlign = Paint.Align.CENTER
            paintTimeInfoText.color = colorRingCurrent
            paintTimeInfoText.alpha = FULL_ALPHA

            prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val savedMode =
                prefs?.let { p ->
                    if (p.contains(ZazenTimerActivity.PREF_KEY_SHOW_ELAPSED_TIME)) {
                        val mode =
                            if (p.getBoolean(ZazenTimerActivity.PREF_KEY_SHOW_ELAPSED_TIME, true)) {
                                SHOW_TIME_MODE_DEFAULT
                            } else {
                                ONE_INT
                            }
                        p.edit().putInt(ZazenTimerActivity.PREF_KEY_SHOW_TIME_MODE, mode).apply()
                        p.edit().remove(ZazenTimerActivity.PREF_KEY_SHOW_ELAPSED_TIME).apply()
                        mode
                    } else {
                        p.getInt(ZazenTimerActivity.PREF_KEY_SHOW_TIME_MODE, SHOW_TIME_MODE_DEFAULT)
                    }
                } ?: SHOW_TIME_MODE_DEFAULT
            showTimeMode = savedMode

            timeTextAnimator = ValueAnimator()
            startTimeInfoFadeOut(DEFAULT_FADE_OUT_MS)
            setOnClickListener {
                showTimeMode = (showTimeMode + ONE_INT) % SHOW_TIME_MODE_MODULUS
                prefs?.edit()?.putInt(ZazenTimerActivity.PREF_KEY_SHOW_TIME_MODE, showTimeMode)?.apply()
                startTimeInfoFadeOut(CLICK_FADE_OUT_MS)
            }
        }

        private fun startTimeInfoFadeOut(durationMs: Int) {
            if (timeTextAnimator.isRunning) {
                timeTextAnimator.end()
            }
            timeTextAnimator.setDuration(durationMs.toLong())
            timeTextAnimator.setFloatValues(FLOAT_ALPHA_RANGE_START, FLOAT_ALPHA_RANGE_END)
            timeTextAnimator.start()
        }

        override fun onSizeChanged(
            w: Int,
            h: Int,
            oldw: Int,
            oldh: Int,
        ) {
            super.onSizeChanged(w, h, oldw, oldh)
            Log.d(TAG, "resize: w=$w h=$h")
            val wf = w.toFloat()
            val hf = h.toFloat()
            if (w >= h) {
                deltaX = (w - h) / HALF_DIVISOR
                deltaY = ZERO_ALPHA
                size = h
            } else {
                deltaX = ZERO_ALPHA
                deltaY = (h - w) / HALF_DIVISOR
                size = w
            }
            val halfW = wf / HALF_DIVISOR_FLOAT
            val halfH = hf / HALF_DIVISOR_FLOAT
            curSectionX = halfW
            curSectionY = halfH
            nextSectionX = halfW
            nextSectionY = (size * NEXT_SECTION_Y_FACTOR) + halfH
            curSectionXStart = curSectionX
            curSectionYStart = curSectionY
            curSectionXEnd = halfW
            curSectionYEnd = halfH - (size * CUR_SECTION_Y_END_FACTOR)
            curSectionAlpha = ALPHA_MAX_FLOAT
            nextSectionXStart = nextSectionX
            nextSectionYStart = nextSectionY
            nextSectionXEnd = curSectionXStart
            nextSectionYEnd = curSectionYStart
            timeInfoSize = size * TIME_INFO_SIZE_FACTOR
            curSectionSize = size * CUR_SECTION_SIZE_FACTOR
            nextSectionSize = size * NEXT_SECTION_SIZE_FACTOR
            nextSectionSizeStart = nextSectionSize
            nextSectionSizeEnd = curSectionSize
            nextSectionColorStart = colorRingNext
            nextSectionColorEnd = colorRingCurrent
            newNextSectionXStart = nextSectionX
            newNextSectionYStart = nextSectionY + (size * NEW_NEXT_Y_OFFSET_FACTOR)
            newNextSectionXEnd = nextSectionX
            newNextSectionYEnd = nextSectionY
            newNextSectionAlpha = ALPHA_MIN_FLOAT
            newNextSectionX = newNextSectionXStart
            newNextSectionY = newNextSectionYStart
            curTimeX = halfW
            curTimeY = halfH - curSectionSize
            timeInfoX = halfW
            timeInfoY = curTimeY - curSectionSize
        }

        @Suppress("LongMethod")
        override fun onDraw(canvas: Canvas) {
            setupPaintAndRect(
                ringWidthPercent,
                size,
                markerWidth,
                colorMarkerLine,
                colorMarkerTriangle,
                colorRingPrev,
                colorRingCurrent,
                colorRingNext,
                colorRingRemaining,
                paddingLeft,
                paddingTop,
                paddingRight,
                paddingBottom,
                deltaX,
                deltaY,
                width,
                height,
                paintPrev,
                paintCurrent,
                paintNext,
                paintRemaining,
                paintMarkerLine,
                paintTriangle,
                rectFull,
            )

            drawRingSegments(
                canvas,
                rectFull,
                angleStart,
                angleEnd,
                floatProperties,
                paintRemaining,
                paintPrev,
                paintCurrent,
                paintNext,
            )

            val ringPx = (ringWidthPercent * size).toInt()
            val halfPx = ringPx / HALF_DIVISOR
            drawMarker(
                canvas,
                rectFull,
                angleStart,
                angleEnd,
                floatProperties,
                ringPx,
                halfPx,
                paintMarkerLine,
                paintTriangle,
                triangle,
            )

            if (isInEditMode) {
                canvas.drawText(EDIT_MODE_TIME_TEXT, curTimeX, curTimeY, paintCurSection)
                canvas.drawText(EDIT_MODE_CURRENT_LABEL, curSectionX, curSectionY, paintCurSection)
                canvas.drawText(EDIT_MODE_NEXT_LABEL, nextSectionX, nextSectionY, paintNextSection)
                canvas.drawText(
                    EDIT_MODE_NEXT_NEXT_LABEL,
                    newNextSectionX,
                    newNextSectionY,
                    paintNewNextSection,
                )
                return
            }

            drawSectionLabels(
                canvas,
                floatProperties,
                interp,
                curSectionXStart,
                curSectionXEnd,
                curSectionYStart,
                curSectionYEnd,
                curSectionSize,
                nextSectionXStart,
                nextSectionXEnd,
                nextSectionYStart,
                nextSectionYEnd,
                nextSectionSizeStart,
                nextSectionSizeEnd,
                nextSectionColorStart,
                nextSectionColorEnd,
                newNextSectionXStart,
                newNextSectionXEnd,
                newNextSectionYStart,
                newNextSectionYEnd,
                colorRingCurrent,
                colorRingNext,
                paintCurSection,
                paintNextSection,
                paintNewNextSection,
                currentSectionName,
                nextSectionName,
                newNextSectionName,
            )

            drawTimeText(
                canvas,
                curTimeX,
                curTimeY,
                curSectionSize,
                timeInfoX,
                timeInfoY,
                timeInfoSize,
                showTimeMode,
                fixedCurrentStartSeconds,
                fixedNextStartSeconds,
                fixedSessionTotalSeconds,
                sectionElapsedSeconds,
                timeTextAnimator,
                paintTimeInfoText,
                paintTimeSectionElapsed,
                paintTimeSectionRemaining,
                paintTimeSessionElapsed,
                paintTimeSessionRemaining,
                context,
            )
        }

        fun animateProperty(
            index: Int,
            value: Int,
        ) {
            when (index) {
                PROP_SESSION_TOTAL_SECONDS -> fixedSessionTotalSeconds = value
                PROP_CURRENT_START_SECONDS -> fixedCurrentStartSeconds = value
                PROP_NEXT_START_SECONDS -> fixedNextStartSeconds = value
            }
            animHelper.animateTo(index, value)
        }

        fun setSectionElapsedSeconds(value: Int) {
            sectionElapsedSeconds = value
        }

        fun setSectionNames(
            currentName: String,
            nextName: String,
        ) {
            val nameChanged = !(currentName == currentSectionName && nextName == nextSectionName)
            if (nameChanged && runningAnims[PROP_NAME_SWITCH] == null) {
                newNextSectionName = nextName
                animHelper.animateTo(
                    PROP_NAME_SWITCH,
                    ONE_INT,
                    SECTION_MORPH_DURATION_MS,
                    Runnable {
                        floatProperties[PROP_NAME_SWITCH] = MORPH_END_VALUE
                        currentSectionName = currentName
                        nextSectionName = nextName
                    },
                )
            }
        }

        fun setSectionNamesNoAnim(
            currentName: String,
            nextName: String,
        ) {
            currentSectionName = currentName
            nextSectionName = nextName
        }

        fun getProperty(index: Int): Float = floatProperties[index]

        fun getSectionElapsedSeconds(): Int = sectionElapsedSeconds
    }
