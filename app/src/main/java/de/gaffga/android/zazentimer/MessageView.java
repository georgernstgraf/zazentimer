package de.gaffga.android.zazentimer;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class MessageView {
    private static final String TAG = "ZMT_MessageView";
    private final Activity activity;
    private String contentText;
    private ViewGroup messageParent;
    private ViewGroup messageView = null;
    private Runnable onOkListener;
    private String titleText;

    public MessageView(Activity activity) {
        this.activity = activity;
    }

    public void setTitle(String str) {
        this.titleText = str;
    }

    public void setText(String str) {
        this.contentText = str;
    }

    public void setOnOkListener(Runnable runnable) {
        this.onOkListener = runnable;
    }

    public void show() {
        if (this.messageView != null) {
            Log.d(TAG, "message already visible");
            return;
        }
        this.messageParent = (ViewGroup) this.activity.findViewById(android.R.id.content);
        this.messageView = (ViewGroup) this.activity.getLayoutInflater().inflate(R.layout.message_view, this.messageParent, false);
        if (Build.VERSION.SDK_INT >= 21) {
            this.messageView.setElevation(50.0f);
        }
        TextView textView = (TextView) this.messageView.findViewById(R.id.message_title);
        TextView textView2 = (TextView) this.messageView.findViewById(R.id.message_text);
        Button button = (Button) this.messageView.findViewById(R.id.message_ok);
        textView.setText(this.titleText);
        textView2.setText(this.contentText);
        this.messageView.setAlpha(0.0f);
        ObjectAnimator objectAnimator = new ObjectAnimator();
        objectAnimator.setTarget(this.messageView);
        objectAnimator.setPropertyName("alpha");
        objectAnimator.setFloatValues(0.0f, 1.0f);
        objectAnimator.setDuration(500L);
        objectAnimator.start();
        this.messageParent.addView(this.messageView);
        button.setOnClickListener(new View.OnClickListener() { // from class: de.gaffga.android.zazentimer.MessageView.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                ObjectAnimator objectAnimator2 = new ObjectAnimator();
                objectAnimator2.setTarget(MessageView.this.messageView);
                objectAnimator2.setPropertyName("alpha");
                objectAnimator2.setFloatValues(1.0f, 0.0f);
                objectAnimator2.setDuration(500L);
                objectAnimator2.addListener(new Animator.AnimatorListener() { // from class: de.gaffga.android.zazentimer.MessageView.1.1
                    @Override // android.animation.Animator.AnimatorListener
                    public void onAnimationCancel(Animator animator) {
                    }

                    @Override // android.animation.Animator.AnimatorListener
                    public void onAnimationRepeat(Animator animator) {
                    }

                    @Override // android.animation.Animator.AnimatorListener
                    public void onAnimationStart(Animator animator) {
                    }

                    @Override // android.animation.Animator.AnimatorListener
                    public void onAnimationEnd(Animator animator) {
                        MessageView.this.messageParent.removeView(MessageView.this.messageView);
                        MessageView.this.messageView = null;
                        if (MessageView.this.onOkListener != null) {
                            MessageView.this.onOkListener.run();
                        }
                    }
                });
                objectAnimator2.start();
            }
        });
    }
}
