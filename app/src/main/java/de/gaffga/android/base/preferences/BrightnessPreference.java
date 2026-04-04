package de.gaffga.android.base.preferences;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import de.gaffga.android.zazentimer.R;

public class BrightnessPreference extends DialogPreference {
    private static final String TAG = "BrightnessPreference";
    private Context context;
    private int currentValue;
    private int oldValue;
    private SeekBar seekBar;

    private void init() {
    }

    public BrightnessPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.context = context;
        setLayout();
        init();
    }

    public BrightnessPreference(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.context = context;
        setLayout();
        init();
    }

    private void setLayout() {
        setDialogLayoutResource(R.layout.preference_volume_slider);
    }

    @Override // android.preference.Preference
    protected void onSetInitialValue(boolean z, Object obj) {
        if (z) {
            this.currentValue = getSharedPreferences().getInt(getKey(), 0);
        } else {
            this.currentValue = ((Integer) obj).intValue();
        }
    }

    @Override // android.preference.DialogPreference
    protected void onDialogClosed(boolean z) {
        if (z) {
            getSharedPreferences().edit().putInt(getKey(), this.currentValue).apply();
        } else {
            this.currentValue = this.oldValue;
        }
        setSummary(this.currentValue + "%");
    }

    @Override // android.preference.Preference
    protected View onCreateView(ViewGroup viewGroup) {
        View onCreateView = super.onCreateView(viewGroup);
        LinearLayout.inflate(getContext(), R.layout.preference_volume_value, (LinearLayout) onCreateView.findViewById(android.R.id.widget_frame));
        this.currentValue = getSharedPreferences().getInt(getKey(), 0);
        this.oldValue = this.currentValue;
        setSummary(this.currentValue + "%");
        return onCreateView;
    }

    @Override // android.preference.DialogPreference
    protected View onCreateDialogView() {
        View onCreateDialogView = super.onCreateDialogView();
        this.seekBar = (SeekBar) onCreateDialogView.findViewById(R.id.volume);
        this.seekBar.setProgress(this.currentValue);
        final TextView textView = (TextView) onCreateDialogView.findViewById(R.id.percent);
        textView.setText(this.currentValue + "%");
        this.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { // from class: de.gaffga.android.base.preferences.BrightnessPreference.1
            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
                BrightnessPreference.this.currentValue = i;
                textView.setText(i + "%");
            }
        });
        this.seekBar.setProgress(this.currentValue);
        return onCreateDialogView;
    }
}
