package de.gaffga.android.zazentimer.views;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.preference.PreferenceManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import de.gaffga.android.zazentimer.R;
import de.gaffga.android.zazentimer.ZazenTimerActivity;
import java.util.HashMap;
import java.util.Locale;

/* loaded from: classes.dex */
public class TimerView extends View {
    public static final int PROP_COUNT = 7;
    public static final int PROP_CURRENT_START_SECONDS = 2;
    public static final int PROP_NAME_SWITCH = 6;
    public static final int PROP_NEXT_END_SECONDS = 4;
    public static final int PROP_NEXT_START_SECONDS = 3;
    public static final int PROP_PREV_START_SECONDS = 1;
    public static final int PROP_SESSION_ELAPSED_SECONDS = 5;
    public static final int PROP_SESSION_TOTAL_SECONDS = 0;
    private static final String TAG = "ZMT_TimerView";
    public static final int TIME_SECTION_ELAPSED = 0;
    public static final int TIME_SECTION_MAX = 3;
    public static final int TIME_SECTION_MIN = 0;
    public static final int TIME_SECTION_REMAINING = 1;
    public static final int TIME_SESSION_ELAPSED = 2;
    public static final int TIME_SESSION_REMAINING = 3;
    private int angleEnd;
    private int angleStart;
    private int animDuration;
    private boolean animating;
    private int colorMarkerLine;
    private int colorMarkerTriangle;
    private int colorRingCurrent;
    private int colorRingNext;
    private int colorRingPrev;
    private int colorRingRemaining;
    private float curSectionAlpha;
    private float curSectionSize;
    private float curSectionX;
    private float curSectionXEnd;
    private float curSectionXStart;
    private float curSectionY;
    private float curSectionYEnd;
    private float curSectionYStart;
    private float curTimeX;
    private float curTimeY;
    private String currentSectionName;
    private int deltaX;
    private int deltaY;
    private int fixedCurrentStartSeconds;
    private int fixedNextStartSeconds;
    private int fixedSessionTotalSeconds;
    private float[] floatProperties;
    private AccelerateDecelerateInterpolator interp;
    private int markerWidth;
    private float newNextSectionAlpha;
    private String newNextSectionName;
    private float newNextSectionX;
    private float newNextSectionXEnd;
    private float newNextSectionXStart;
    private float newNextSectionY;
    private float newNextSectionYEnd;
    private float newNextSectionYStart;
    private int nextSectionColor;
    private int nextSectionColorEnd;
    private int nextSectionColorStart;
    private String nextSectionName;
    private float nextSectionSize;
    private float nextSectionSizeEnd;
    private float nextSectionSizeStart;
    private float nextSectionX;
    private float nextSectionXEnd;
    private float nextSectionXStart;
    private float nextSectionY;
    private float nextSectionYEnd;
    private float nextSectionYStart;
    private int paddingBottom;
    private int paddingLeft;
    private int paddingRight;
    private int paddingTop;
    private Paint paintCurSection;
    private Paint paintCurrent;
    private Paint paintGrayLine;
    private Paint paintMarkerLine;
    private Paint paintNewNextSection;
    private Paint paintNext;
    private Paint paintNextSection;
    private Paint paintPrev;
    private Paint paintRemaining;
    private Paint paintTimeInfoText;
    private Paint paintTimeSectionElapsed;
    private Paint paintTimeSectionRemaining;
    private Paint paintTimeSessionElapsed;
    private Paint paintTimeSessionRemaining;
    private Paint paintTriangle;
    private SharedPreferences prefs;
    private RectF rectFull;
    private float ringWidthPercent;
    private HashMap<Integer, MyAnimator> runningAnims;
    private int sectionElapsedSeconds;
    private int showTimeMode;
    private int size;
    private float timeInfoSize;
    private float timeInfoX;
    private float timeInfoY;
    private ValueAnimator timeTextAnimator;
    private Path triangle;

    private int morphColor(int i, int i2, float f) {
        int a1 = (i >> 24) & 255;
        int r1 = (i >> 16) & 255;
        int g1 = (i >> 8) & 255;
        int b1 = i & 255;
        
        int a2 = (i2 >> 24) & 255;
        int r2 = (i2 >> 16) & 255;
        int g2 = (i2 >> 8) & 255;
        int b2 = i2 & 255;
        
        int a = a1 + (int) ((a2 - a1) * f);
        int r = r1 + (int) ((r2 - r1) * f);
        int g = g1 + (int) ((g2 - g1) * f);
        int b = b1 + (int) ((b2 - b1) * f);
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    static /* synthetic */ int access$008(TimerView timerView) {
        int i = timerView.showTimeMode;
        timerView.showTimeMode = i + 1;
        return i;
    }

    public TimerView(Context context) {
        this(context, null);
    }

    public TimerView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R.attr.timerViewStyle);
    }

    public TimerView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.rectFull = new RectF();
        this.paddingLeft = 40;
        this.paddingRight = 40;
        this.paddingTop = 40;
        this.paddingBottom = 40;
        this.markerWidth = 6;
        this.ringWidthPercent = 0.08f;
        this.colorMarkerLine = -12566464;
        this.colorMarkerTriangle = -12566464;
        this.colorRingPrev = -3282705;
        this.colorRingCurrent = -12746077;
        this.colorRingNext = -6499103;
        this.colorRingRemaining = -986896;
        this.angleStart = 120;
        this.angleEnd = 420;
        this.interp = new AccelerateDecelerateInterpolator();
        this.triangle = new Path();
        this.paintGrayLine = new Paint();
        this.paintMarkerLine = new Paint();
        this.paintTriangle = new Paint();
        this.paintPrev = new Paint();
        this.paintCurrent = new Paint();
        this.paintNext = new Paint();
        this.paintRemaining = new Paint();
        this.paintCurSection = new Paint();
        this.paintNewNextSection = new Paint();
        this.paintNextSection = new Paint();
        this.paintTimeSectionElapsed = new Paint();
        this.paintTimeSectionRemaining = new Paint();
        this.paintTimeSessionElapsed = new Paint();
        this.paintTimeSessionRemaining = new Paint();
        this.paintTimeInfoText = new Paint();
        this.showTimeMode = 0;
        this.floatProperties = new float[7];
        this.currentSectionName = "";
        this.nextSectionName = "";
        this.newNextSectionName = "";
        this.animDuration = ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION;
        this.runningAnims = new HashMap<>();
        init(attributeSet, i, 0);
    }

    @TargetApi(21)
    public TimerView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.rectFull = new RectF();
        this.paddingLeft = 40;
        this.paddingRight = 40;
        this.paddingTop = 40;
        this.paddingBottom = 40;
        this.markerWidth = 6;
        this.ringWidthPercent = 0.08f;
        this.colorMarkerLine = -12566464;
        this.colorMarkerTriangle = -12566464;
        this.colorRingPrev = -3282705;
        this.colorRingCurrent = -12746077;
        this.colorRingNext = -6499103;
        this.colorRingRemaining = -986896;
        this.angleStart = 120;
        this.angleEnd = 420;
        this.interp = new AccelerateDecelerateInterpolator();
        this.triangle = new Path();
        this.paintGrayLine = new Paint();
        this.paintMarkerLine = new Paint();
        this.paintTriangle = new Paint();
        this.paintPrev = new Paint();
        this.paintCurrent = new Paint();
        this.paintNext = new Paint();
        this.paintRemaining = new Paint();
        this.paintCurSection = new Paint();
        this.paintNewNextSection = new Paint();
        this.paintNextSection = new Paint();
        this.paintTimeSectionElapsed = new Paint();
        this.paintTimeSectionRemaining = new Paint();
        this.paintTimeSessionElapsed = new Paint();
        this.paintTimeSessionRemaining = new Paint();
        this.paintTimeInfoText = new Paint();
        this.showTimeMode = 0;
        this.floatProperties = new float[7];
        this.currentSectionName = "";
        this.nextSectionName = "";
        this.newNextSectionName = "";
        this.animDuration = ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION;
        this.runningAnims = new HashMap<>();
        init(attributeSet, i, i2);
    }

    private void init(AttributeSet attributeSet, int i, int i2) {
        TypedArray obtainStyledAttributes = getContext().obtainStyledAttributes(attributeSet, R.styleable.TimerView, i, i2);
        this.colorRingPrev = obtainStyledAttributes.getColor(4, -3282705);
        this.colorRingCurrent = obtainStyledAttributes.getColor(2, -12746077);
        this.colorRingNext = obtainStyledAttributes.getColor(3, -6499103);
        this.colorRingRemaining = obtainStyledAttributes.getColor(5, -986896);
        this.colorMarkerLine = obtainStyledAttributes.getColor(0, -12566464);
        this.colorMarkerTriangle = obtainStyledAttributes.getColor(1, -12566464);
        this.paintGrayLine.setStrokeWidth(2.0f);
        this.paintGrayLine.setStyle(Paint.Style.STROKE);
        this.paintGrayLine.setColor(-12566464);
        this.paintTimeSectionElapsed.setAntiAlias(true);
        this.paintTimeSectionElapsed.setTextAlign(Paint.Align.CENTER);
        this.paintTimeSectionElapsed.setColor(getColorRingCurrent());
        this.paintTimeSectionElapsed.setAlpha(255);
        this.paintTimeSectionRemaining.setAntiAlias(true);
        this.paintTimeSectionRemaining.setTextAlign(Paint.Align.CENTER);
        this.paintTimeSectionRemaining.setColor(getColorRingCurrent());
        this.paintTimeSectionRemaining.setAlpha(255);
        this.paintTimeSessionElapsed.setAntiAlias(true);
        this.paintTimeSessionElapsed.setTextAlign(Paint.Align.CENTER);
        this.paintTimeSessionElapsed.setColor(getColorRingCurrent());
        this.paintTimeSessionElapsed.setAlpha(255);
        this.paintTimeSessionRemaining.setAntiAlias(true);
        this.paintTimeSessionRemaining.setTextAlign(Paint.Align.CENTER);
        this.paintTimeSessionRemaining.setColor(getColorRingCurrent());
        this.paintTimeSessionRemaining.setAlpha(255);
        this.paintTimeInfoText.setAntiAlias(true);
        this.paintTimeInfoText.setTextAlign(Paint.Align.CENTER);
        this.paintTimeInfoText.setColor(getColorRingCurrent());
        this.paintTimeInfoText.setAlpha(255);
        this.prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        if (this.prefs.contains(ZazenTimerActivity.PREF_KEY_SHOW_ELAPSED_TIME)) {
            if (this.prefs.getBoolean(ZazenTimerActivity.PREF_KEY_SHOW_ELAPSED_TIME, true)) {
                this.prefs.edit().putInt(ZazenTimerActivity.PREF_KEY_SHOW_TIME_MODE, 0).apply();
            } else {
                this.prefs.edit().putInt(ZazenTimerActivity.PREF_KEY_SHOW_TIME_MODE, 1).apply();
            }
            this.prefs.edit().remove(ZazenTimerActivity.PREF_KEY_SHOW_ELAPSED_TIME).apply();
        }
        this.showTimeMode = this.prefs.getInt(ZazenTimerActivity.PREF_KEY_SHOW_TIME_MODE, 0);
        this.timeTextAnimator = new ValueAnimator();
        startTimeInfoFadeOut(7000);
        setOnClickListener(new View.OnClickListener() { // from class: de.gaffga.android.zazentimer.views.TimerView.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                TimerView.access$008(TimerView.this);
                if (TimerView.this.showTimeMode > 3) {
                    TimerView.this.showTimeMode = 0;
                }
                TimerView.this.prefs.edit().putInt(ZazenTimerActivity.PREF_KEY_SHOW_TIME_MODE, TimerView.this.showTimeMode).apply();
                TimerView.this.startTimeInfoFadeOut(3000);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startTimeInfoFadeOut(int i) {
        if (this.timeTextAnimator.isRunning()) {
            this.timeTextAnimator.end();
        }
        this.timeTextAnimator.setDuration(i);
        this.timeTextAnimator.setFloatValues(255.0f, 0.0f);
        this.timeTextAnimator.start();
    }

    @Override // android.view.View
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        Log.d(TAG, "resize: w=" + i + " h=" + i2);
        if (i >= i2) {
            this.deltaX = (i - i2) / 2;
            this.deltaY = 0;
            this.size = i2;
        } else {
            this.deltaX = 0;
            this.deltaY = (i2 - i) / 2;
            this.size = i;
        }
        float f = i / 2;
        this.curSectionX = f;
        float f2 = i2 / 2;
        this.curSectionY = f2;
        this.nextSectionX = f;
        this.nextSectionY = (this.size * 0.15f) + f2;
        this.curSectionXStart = this.curSectionX;
        this.curSectionYStart = this.curSectionY;
        this.curSectionXEnd = f;
        this.curSectionYEnd = f2 - (this.size * 0.25f);
        this.curSectionAlpha = 1.0f;
        this.nextSectionXStart = this.nextSectionX;
        this.nextSectionYStart = this.nextSectionY;
        this.nextSectionXEnd = this.curSectionXStart;
        this.nextSectionYEnd = this.curSectionYStart;
        this.timeInfoSize = this.size * 0.05f;
        this.curSectionSize = this.size * 0.1f;
        this.nextSectionSize = this.size * 0.07f;
        this.nextSectionSizeStart = this.nextSectionSize;
        this.nextSectionSizeEnd = this.curSectionSize;
        this.nextSectionColorStart = getColorRingNext();
        this.nextSectionColorEnd = getColorRingCurrent();
        this.newNextSectionXStart = this.nextSectionX;
        this.newNextSectionYStart = this.nextSectionY + (this.size * 0.25f);
        this.newNextSectionXEnd = this.nextSectionX;
        this.newNextSectionYEnd = this.nextSectionY;
        this.newNextSectionAlpha = 0.0f;
        this.newNextSectionX = this.newNextSectionXStart;
        this.newNextSectionY = this.newNextSectionYStart;
        this.curTimeX = f;
        this.curTimeY = f2 - this.curSectionSize;
        this.timeInfoX = f;
        this.timeInfoY = this.curTimeY - this.curSectionSize;
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        int i = (int) (this.ringWidthPercent * this.size);
        float f = i;
        int i2 = (int) (f * 1.0f);
        int i3 = i2 / 2;
        this.paintTimeSectionElapsed.setTextSize(this.curSectionSize);
        this.paintTimeSectionRemaining.setTextSize(this.curSectionSize);
        this.paintTimeSessionElapsed.setTextSize(this.curSectionSize);
        this.paintTimeSessionRemaining.setTextSize(this.curSectionSize);
        this.paintTimeInfoText.setTextSize(this.timeInfoSize);
        this.paintMarkerLine.setStyle(Paint.Style.STROKE);
        this.paintMarkerLine.setAntiAlias(true);
        this.paintMarkerLine.setStrokeWidth(this.markerWidth);
        this.paintMarkerLine.setColor(getColorMarkerLine());
        this.paintTriangle.setStyle(Paint.Style.FILL);
        this.paintTriangle.setAntiAlias(true);
        this.paintTriangle.setColor(getColorMarkerTriangle());
        this.paintPrev.setStrokeWidth(f);
        this.paintPrev.setAntiAlias(true);
        this.paintPrev.setStyle(Paint.Style.STROKE);
        this.paintPrev.setColor(getColorRingPrev());
        this.paintCurrent.setStrokeWidth(f);
        this.paintCurrent.setAntiAlias(true);
        this.paintCurrent.setStyle(Paint.Style.STROKE);
        this.paintCurrent.setColor(getColorRingCurrent());
        this.paintNext.setStrokeWidth(f);
        this.paintNext.setAntiAlias(true);
        this.paintNext.setStyle(Paint.Style.STROKE);
        this.paintNext.setColor(getColorRingNext());
        this.paintRemaining.setStrokeWidth(f);
        this.paintRemaining.setAntiAlias(true);
        this.paintRemaining.setStyle(Paint.Style.STROKE);
        this.paintRemaining.setColor(getColorRingRemaining());
        int i4 = i / 2;
        this.rectFull.set(this.paddingLeft + i4 + this.deltaX, this.paddingTop + i4 + this.deltaY, (width - (this.paddingRight + i4)) - this.deltaX, (height - (this.paddingBottom + i4)) - this.deltaY);
        float f2 = this.angleStart;
        canvas.drawArc(this.rectFull, f2, (this.angleStart + (((this.angleEnd - this.angleStart) * getPrevStartSeconds()) / getSessionTotalSeconds())) - f2, false, this.paintRemaining);
        float prevStartSeconds = this.angleStart + (((this.angleEnd - this.angleStart) * getPrevStartSeconds()) / getSessionTotalSeconds());
        canvas.drawArc(this.rectFull, prevStartSeconds, (this.angleStart + (((this.angleEnd - this.angleStart) * getCurrentStartSeconds()) / getSessionTotalSeconds())) - prevStartSeconds, false, this.paintPrev);
        float currentStartSeconds = this.angleStart + (((this.angleEnd - this.angleStart) * getCurrentStartSeconds()) / getSessionTotalSeconds());
        canvas.drawArc(this.rectFull, currentStartSeconds, (this.angleStart + (((this.angleEnd - this.angleStart) * getNextStartSeconds()) / getSessionTotalSeconds())) - currentStartSeconds, false, this.paintCurrent);
        float nextStartSeconds = this.angleStart + (((this.angleEnd - this.angleStart) * getNextStartSeconds()) / getSessionTotalSeconds());
        canvas.drawArc(this.rectFull, nextStartSeconds, (this.angleStart + (((this.angleEnd - this.angleStart) * getNextEndSeconds()) / getSessionTotalSeconds())) - nextStartSeconds, false, this.paintNext);
        float nextEndSeconds = this.angleStart + (((this.angleEnd - this.angleStart) * getNextEndSeconds()) / getSessionTotalSeconds());
        canvas.drawArc(this.rectFull, nextEndSeconds, this.angleEnd - nextEndSeconds, false, this.paintRemaining);
        double sessionElapsedSeconds = ((this.angleStart + (((this.angleEnd - this.angleStart) * getSessionElapsedSeconds()) / getSessionTotalSeconds())) * 3.1415f) / 180.0f;
        float cos = (float) Math.cos(sessionElapsedSeconds);
        float sin = (float) Math.sin(sessionElapsedSeconds);
        float width2 = ((this.rectFull.width() * cos) / 2.0f) + this.rectFull.centerX();
        float height2 = ((this.rectFull.height() * sin) / 2.0f) + this.rectFull.centerY();
        float f3 = i2;
        float f4 = (cos * f3) / 2.0f;
        float f5 = width2 + f4;
        float f6 = (f3 * sin) / 2.0f;
        float f7 = height2 + f6;
        float f8 = width2 - f4;
        float f9 = height2 - f6;
        float f10 = i3;
        float f11 = cos * f10;
        float f12 = f5 + f11;
        float f13 = sin * f10;
        float f14 = f12 - f13;
        float f15 = f7 + f13;
        float f16 = f15 + f11;
        float f17 = f12 + f13;
        float f18 = f15 - f11;
        float f19 = f8 - f11;
        float f20 = f19 - f13;
        float f21 = f9 - f13;
        float f22 = f21 + f11;
        float f23 = f19 + f13;
        float f24 = f21 - f11;
        float f25 = f11 * 0.5f;
        float f26 = f13 * 0.5f;
        canvas.drawLine(f5 + f25, f7 + f26, f8 - f25, f9 - f26, this.paintMarkerLine);
        this.triangle.reset();
        this.triangle.moveTo(f5, f7);
        this.triangle.lineTo(f14, f16);
        this.triangle.lineTo(f17, f18);
        this.triangle.close();
        canvas.drawPath(this.triangle, this.paintTriangle);
        this.triangle.reset();
        this.triangle.moveTo(f8, f9);
        this.triangle.lineTo(f20, f22);
        this.triangle.lineTo(f23, f24);
        this.triangle.close();
        canvas.drawPath(this.triangle, this.paintTriangle);
        float interpolation = this.interp.getInterpolation(this.floatProperties[6]);
        this.curSectionX = this.curSectionXStart + ((this.curSectionXEnd - this.curSectionXStart) * interpolation);
        this.curSectionY = this.curSectionYStart + ((this.curSectionYEnd - this.curSectionYStart) * interpolation);
        this.nextSectionX = this.nextSectionXStart + ((this.nextSectionXEnd - this.nextSectionXStart) * interpolation);
        this.nextSectionY = this.nextSectionYStart + ((this.nextSectionYEnd - this.nextSectionYStart) * interpolation);
        this.curSectionAlpha = 1.0f - interpolation;
        this.nextSectionSize = this.nextSectionSizeStart + ((this.nextSectionSizeEnd - this.nextSectionSizeStart) * interpolation);
        this.newNextSectionX = this.newNextSectionXStart + ((this.newNextSectionXEnd - this.newNextSectionXStart) * interpolation);
        this.newNextSectionY = this.newNextSectionYStart + ((this.newNextSectionYEnd - this.newNextSectionYStart) * interpolation);
        this.newNextSectionAlpha = interpolation;
        this.nextSectionColor = morphColor(this.nextSectionColorStart, this.nextSectionColorEnd, interpolation);
        this.paintCurSection.setAntiAlias(true);
        this.paintCurSection.setTextAlign(Paint.Align.CENTER);
        this.paintCurSection.setTextSize(this.curSectionSize);
        this.paintCurSection.setColor(getColorRingCurrent());
        this.paintCurSection.setAlpha((int) (this.curSectionAlpha * 255.0f));
        this.paintNewNextSection.setAntiAlias(true);
        this.paintNewNextSection.setTextAlign(Paint.Align.CENTER);
        this.paintNewNextSection.setTextSize(this.nextSectionSizeStart);
        this.paintNewNextSection.setColor(getColorRingNext());
        this.paintNewNextSection.setAlpha((int) (this.newNextSectionAlpha * 255.0f));
        this.paintNextSection.setAntiAlias(true);
        this.paintNextSection.setTextAlign(Paint.Align.CENTER);
        this.paintNextSection.setTextSize(this.nextSectionSize);
        this.paintNextSection.setColor(this.nextSectionColor);
        this.paintNextSection.setAlpha(255);
        if (isInEditMode()) {
            canvas.drawText("05:20", this.curTimeX, this.curTimeY, this.paintCurSection);
            canvas.drawText("current sec", this.curSectionX, this.curSectionY, this.paintCurSection);
            canvas.drawText("next sec", this.nextSectionX, this.nextSectionY, this.paintNextSection);
            canvas.drawText("next next sec", this.newNextSectionX, this.newNextSectionY, this.paintNewNextSection);
            return;
        }
        int sectionElapsedSeconds = getSectionElapsedSeconds();
        String str = "";
        Paint paint = null;
        String str2 = "";
        if (this.showTimeMode == 0) {
            str = String.format(Locale.getDefault(), "%1$02d:%2$02d", Integer.valueOf(sectionElapsedSeconds / 60), Integer.valueOf(sectionElapsedSeconds % 60));
            paint = this.paintTimeSectionElapsed;
            str2 = getContext().getString(R.string.time_section_elapsed);
        } else if (this.showTimeMode == 1) {
            int i5 = (this.fixedNextStartSeconds - this.fixedCurrentStartSeconds) - sectionElapsedSeconds;
            str = String.format(Locale.getDefault(), "-%1$02d:%2$02d", Integer.valueOf(i5 / 60), Integer.valueOf(i5 % 60));
            paint = this.paintTimeSectionRemaining;
            str2 = getContext().getString(R.string.time_section_remaining);
        } else if (this.showTimeMode == 2) {
            int i6 = this.fixedCurrentStartSeconds + sectionElapsedSeconds;
            str = String.format(Locale.getDefault(), "%1$02d:%2$02d", Integer.valueOf(i6 / 60), Integer.valueOf(i6 % 60));
            paint = this.paintTimeSessionElapsed;
            str2 = getContext().getString(R.string.time_session_elapsed);
        } else if (this.showTimeMode == 3) {
            int i7 = this.fixedSessionTotalSeconds - (this.fixedCurrentStartSeconds + sectionElapsedSeconds);
            str = String.format(Locale.getDefault(), "-%1$02d:%2$02d", Integer.valueOf(i7 / 60), Integer.valueOf(i7 % 60));
            paint = this.paintTimeSessionRemaining;
            str2 = getContext().getString(R.string.time_session_remaining);
        }
        if (this.timeTextAnimator.isRunning()) {
            this.paintTimeInfoText.setAlpha((int) ((Float) this.timeTextAnimator.getAnimatedValue()).floatValue());
        }
        canvas.drawText(str2, this.timeInfoX, this.timeInfoY, this.paintTimeInfoText);
        canvas.drawText(str, this.curTimeX, this.curTimeY, paint);
        canvas.drawText(this.currentSectionName, this.curSectionX, this.curSectionY, this.paintCurSection);
        canvas.drawText(this.nextSectionName, this.nextSectionX, this.nextSectionY, this.paintNextSection);
        canvas.drawText(this.newNextSectionName, this.newNextSectionX, this.newNextSectionY, this.paintNewNextSection);
    }

    private void startAnimation() {
        if (this.animating) {
            return;
        }
        this.animating = true;
        postDelayed(new ChangeAnimation(), 15L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ChangeAnimation implements Runnable {
        private ChangeAnimation() {
        }

        @Override // java.lang.Runnable
        public void run() {
            boolean z = false;
            for (int i = 0; i < 7; i++) {
                MyAnimator myAnimator = (MyAnimator) TimerView.this.runningAnims.get(Integer.valueOf(i));
                if (myAnimator != null) {
                    float currentTimeMillis = ((float) (System.currentTimeMillis() - myAnimator.startTime)) / myAnimator.duration;
                    if (currentTimeMillis >= 0.0f && currentTimeMillis <= 1.0f) {
                        TimerView.this.floatProperties[i] = ((myAnimator.toVal - myAnimator.fromVal) * currentTimeMillis) + myAnimator.fromVal;
                    } else if (currentTimeMillis > 1.0f) {
                        if (myAnimator.runOnEnd == null) {
                            TimerView.this.floatProperties[i] = myAnimator.toVal;
                        } else {
                            myAnimator.runOnEnd.run();
                        }
                        TimerView.this.runningAnims.remove(Integer.valueOf(i));
                    }
                    z = true;
                }
            }
            TimerView.this.postInvalidate();
            if (!z) {
                TimerView.this.animating = false;
            } else {
                TimerView.this.postDelayed(this, 15L);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MyAnimator {
        public int duration;
        public float fromVal;
        public int propertyIdx;
        public Runnable runOnEnd;
        public long startTime;
        public float toVal;

        private MyAnimator() {
        }
    }

    public void animateTo(int i, int i2, int i3) {
        animateTo(i, i2, i3, null);
    }

    public void animateTo(int i, int i2, int i3, Runnable runnable) {
        MyAnimator myAnimator = this.runningAnims.get(Integer.valueOf(i));
        if (myAnimator == null) {
            myAnimator = new MyAnimator();
        }
        myAnimator.runOnEnd = runnable;
        myAnimator.propertyIdx = i;
        myAnimator.duration = i3;
        myAnimator.toVal = i2;
        myAnimator.fromVal = this.floatProperties[i];
        myAnimator.startTime = System.currentTimeMillis();
        this.runningAnims.put(Integer.valueOf(i), myAnimator);
        if (this.animating) {
            return;
        }
        startAnimation();
    }

    public void setNumTotalSeconds(int i) {
        this.fixedSessionTotalSeconds = i;
        animateTo(0, i, this.animDuration);
    }

    public void setPrevStartSeconds(int i) {
        animateTo(1, i, this.animDuration);
    }

    public void setCurrentStartSeconds(int i) {
        this.fixedCurrentStartSeconds = i;
        animateTo(2, i, this.animDuration);
    }

    public void setNextStartSeconds(int i) {
        this.fixedNextStartSeconds = i;
        animateTo(3, i, this.animDuration);
    }

    public void setNextEndSeconds(int i) {
        animateTo(4, i, this.animDuration);
    }

    public void setSessionElapsedSeconds(int i) {
        animateTo(5, i, this.animDuration);
    }

    public void setSectionElapsedSeconds(int i) {
        this.sectionElapsedSeconds = i;
    }

    public void setSectionNames(final String str, final String str2, String str3) {
        if (((str.equals(this.currentSectionName) && str2.equals(this.nextSectionName)) ? false : true) && this.runningAnims.get(6) == null) {
            this.newNextSectionName = str2;
            animateTo(6, 1, 1400, new Runnable() { // from class: de.gaffga.android.zazentimer.views.TimerView.2
                @Override // java.lang.Runnable
                public void run() {
                    TimerView.this.floatProperties[6] = 0.0f;
                    TimerView.this.currentSectionName = str;
                    TimerView.this.nextSectionName = str2;
                }
            });
        }
    }

    public void setSectionNamesNoAnim(String str, String str2) {
        this.currentSectionName = str;
        this.nextSectionName = str2;
    }

    public int getColorMarkerLine() {
        return this.colorMarkerLine;
    }

    public int getColorMarkerTriangle() {
        return this.colorMarkerTriangle;
    }

    public int getColorRingPrev() {
        return this.colorRingPrev;
    }

    public int getColorRingCurrent() {
        return this.colorRingCurrent;
    }

    public int getColorRingNext() {
        return this.colorRingNext;
    }

    public int getColorRingRemaining() {
        return this.colorRingRemaining;
    }

    public float getPrevStartSeconds() {
        return this.floatProperties[1];
    }

    public float getSessionTotalSeconds() {
        return this.floatProperties[0];
    }

    public float getCurrentStartSeconds() {
        return this.floatProperties[2];
    }

    public float getNextStartSeconds() {
        return this.floatProperties[3];
    }

    public float getNextEndSeconds() {
        return this.floatProperties[4];
    }

    public float getSessionElapsedSeconds() {
        return this.floatProperties[5];
    }

    public int getSectionElapsedSeconds() {
        return this.sectionElapsedSeconds;
    }
}
