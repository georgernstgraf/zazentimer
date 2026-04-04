package de.gaffga.betterlist;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import android.util.TypedValue;
import android.view.View;
import de.gaffga.android.zazentimer.R;

/* loaded from: classes.dex */
public class BitmapView {
    private final int backgroundColor;
    private Bitmap bmp;
    private boolean drawShadow;
    private int fitToHeight;
    private float insetFactor;
    private View parent;
    private float x;
    private float y;
    private ObjectAnimator animAlpha = new ObjectAnimator();
    private ObjectAnimator animX = new ObjectAnimator();
    private ObjectAnimator animY = new ObjectAnimator();
    private Paint paint = new Paint();
    private Rect fitRect = new Rect();
    private float alpha = 1.0f;

    public BitmapView(View view, int i) {
        this.parent = view;
        this.backgroundColor = i;
    }

    public BitmapView(View view, Bitmap bitmap, float f, float f2, int i) {
        this.parent = view;
        this.bmp = bitmap;
        this.x = f;
        this.y = f2;
        this.backgroundColor = i;
    }

    public Bitmap getBmp() {
        return this.bmp;
    }

    public void setBmp(Bitmap bitmap) {
        this.bmp = bitmap;
    }

    public float getX() {
        return this.x;
    }

    public void setX(float f) {
        this.x = f;
        this.parent.postInvalidate();
    }

    public float getY() {
        return this.y;
    }

    public void setY(float f) {
        this.y = f;
        this.parent.postInvalidate();
    }

    public float getAlpha() {
        return this.alpha;
    }

    public void setAlpha(float f) {
        this.alpha = f;
        this.parent.postInvalidate();
    }

    public void animateAlphaTo(float f, long j) {
        animateAlphaTo(f, j, null);
    }

    public void animateAlphaTo(float f, long j, final Runnable runnable) {
        if (this.animAlpha.isRunning()) {
            this.animAlpha.cancel();
        }
        this.animAlpha.setTarget(this);
        this.animAlpha.setPropertyName("alpha");
        this.animAlpha.setFloatValues(f);
        this.animAlpha.setDuration(j);
        this.animAlpha.removeAllListeners();
        this.animAlpha.addListener(new AnimatorListenerAdapter() { // from class: de.gaffga.betterlist.BitmapView.1
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                if (runnable != null) {
                    runnable.run();
                }
            }
        });
        this.animAlpha.start();
    }

    public void animateMoveTo(float f, float f2, long j) {
        animateMoveTo(f, f2, j, null);
    }

    public void animateMoveTo(float f, float f2, long j, final Runnable runnable) {
        if (this.animX.isRunning() || this.animY.isRunning()) {
            this.animX.cancel();
            this.animY.cancel();
        }
        this.animX.setTarget(this);
        this.animX.setPropertyName("x");
        this.animX.setFloatValues(f);
        this.animX.setDuration(j);
        this.animY.setTarget(this);
        this.animY.setPropertyName("y");
        this.animY.setFloatValues(f2);
        this.animY.setDuration(j);
        this.animY.removeAllListeners();
        this.animY.addListener(new AnimatorListenerAdapter() { // from class: de.gaffga.betterlist.BitmapView.2
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationCancel(Animator animator) {
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationStart(Animator animator) {
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                if (runnable != null) {
                    runnable.run();
                }
            }
        });
        this.animX.start();
        this.animY.start();
    }

    public void stop() {
        if (this.animX.isRunning() || this.animY.isRunning()) {
            this.animX.cancel();
            this.animY.cancel();
        }
    }

    public int getWidth() {
        return this.bmp.getWidth();
    }

    public int getHeight() {
        return this.bmp.getHeight();
    }

    public void fitToHeight(int i) {
        this.fitToHeight = i;
    }

    public void setInsertFactor(float f) {
        this.insetFactor = f;
    }

    public void draw(Canvas canvas) {
        if (this.drawShadow) {
            int applyDimension = (int) TypedValue.applyDimension(1, 20.0f, this.parent.getResources().getDisplayMetrics());
            Drawable drawable = ContextCompat.getDrawable(this.parent.getContext(), R.drawable.shadow);
            int i = (int) this.x;
            int i2 = (int) this.y;
            drawable.setBounds(i - applyDimension, i2 - applyDimension, this.bmp.getWidth() + i + applyDimension, this.bmp.getHeight() + i2 + applyDimension);
            drawable.draw(canvas);
            this.paint.setColor(this.backgroundColor);
            canvas.drawRect((int) this.x, (int) this.y, ((int) this.x) + this.bmp.getWidth(), ((int) this.y) + this.bmp.getHeight(), this.paint);
        }
        this.paint.setAlpha((int) (this.alpha * 255.0f));
        if (this.fitToHeight == 0) {
            canvas.drawBitmap(this.bmp, this.x, this.y, this.paint);
            return;
        }
        this.fitRect.set((int) this.x, (int) this.y, ((int) this.x) + ((this.bmp.getWidth() * this.fitToHeight) / this.bmp.getHeight()), ((int) this.y) + this.fitToHeight);
        this.fitRect.inset((int) (this.fitRect.width() * this.insetFactor), (int) (this.fitRect.height() * this.insetFactor));
        canvas.drawBitmap(this.bmp, (Rect) null, this.fitRect, this.paint);
    }

    public void setDrawShadow(boolean z) {
        this.drawShadow = z;
    }
}
