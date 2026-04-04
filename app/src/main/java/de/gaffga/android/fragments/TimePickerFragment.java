package de.gaffga.android.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.TimePicker;
import de.gaffga.android.zazentimer.R;

/* loaded from: classes.dex */
public class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {
    private Activity activity;
    private int minutes;
    private NumberPicker npMin;
    private NumberPicker npSec;
    private Runnable onOkRunnable;
    private int seconds;
    private View view;

    @Override // android.app.TimePickerDialog.OnTimeSetListener
    public void onTimeSet(TimePicker timePicker, int i, int i2) {
    }

    public void setOnOkListener(Runnable runnable) {
        this.onOkRunnable = runnable;
    }

    public int getSeconds() {
        return this.npSec.getValue();
    }

    public void setSeconds(int i) {
        this.seconds = i;
        if (this.npSec != null) {
            this.npSec.setValue(i);
        }
    }

    public int getMinutes() {
        return this.npMin.getValue();
    }

    public void setMinutes(int i) {
        this.minutes = i;
        if (this.npMin != null) {
            this.npMin.setValue(i);
        }
    }

    @Override // android.app.DialogFragment
    public Dialog onCreateDialog(Bundle bundle) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.activity);
        this.view = this.activity.getLayoutInflater().inflate(R.layout.dialog_time_picker, (ViewGroup) null);
        builder.setView(this.view);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() { // from class: de.gaffga.android.fragments.TimePickerFragment.1
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
                TimePickerFragment.this.seconds = TimePickerFragment.this.npSec.getValue();
                TimePickerFragment.this.minutes = TimePickerFragment.this.npMin.getValue();
                if (TimePickerFragment.this.onOkRunnable != null) {
                    TimePickerFragment.this.onOkRunnable.run();
                }
            }
        });
        builder.setNegativeButton(R.string.abbrechen, new DialogInterface.OnClickListener() { // from class: de.gaffga.android.fragments.TimePickerFragment.2
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        AlertDialog create = builder.create();
        this.npMin = (NumberPicker) this.view.findViewById(R.id.pickerMinutes);
        this.npSec = (NumberPicker) this.view.findViewById(R.id.pickerSeconds);
        this.npMin.setMinValue(0);
        this.npMin.setMaxValue(120);
        this.npMin.setValue(this.minutes);
        this.npSec.setMinValue(0);
        this.npSec.setMaxValue(59);
        this.npSec.setValue(this.seconds);
        return create;
    }

    @Override // android.app.Fragment
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
    }
}
