package at.priv.graf.zazentimer.fragments

import android.app.Activity
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.TimePicker
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import at.priv.graf.zazentimer.R

class TimePickerFragment :
    DialogFragment(),
    TimePickerDialog.OnTimeSetListener {
    private var activity: Activity? = null
    private var minutes: Int = 0
    private var npMin: NumberPicker? = null
    private var npSec: NumberPicker? = null
    private var onOkRunnable: Runnable? = null
    private var seconds: Int = 0
    private var view: View? = null

    override fun onTimeSet(
        timePicker: TimePicker,
        i: Int,
        i2: Int,
    ) {
    }

    fun setOnOkListener(runnable: Runnable) {
        this.onOkRunnable = runnable
    }

    fun getSeconds(): Int = npSec?.value ?: seconds

    fun setSeconds(i: Int) {
        this.seconds = i
        npSec?.value = i
    }

    fun getMinutes(): Int = npMin?.value ?: minutes

    fun setMinutes(i: Int) {
        this.minutes = i
        npMin?.value = i
    }

    override fun onCreateDialog(bundle: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val act = activity ?: return super.onCreateDialog(bundle)
        this.view = act.layoutInflater.inflate(R.layout.dialog_time_picker, null as ViewGroup?)
        builder.setView(this.view)
        builder.setPositiveButton(R.string.ok) { _: DialogInterface, _: Int ->
            npSec?.let { this@TimePickerFragment.seconds = it.value }
            npMin?.let { this@TimePickerFragment.minutes = it.value }
            onOkRunnable?.run()
        }
        builder.setNegativeButton(R.string.abbrechen) { _: DialogInterface, _: Int ->
        }
        val create = builder.create()
        val v = this.view ?: return create
        this.npMin = v.findViewById(R.id.pickerMinutes)
        this.npSec = v.findViewById(R.id.pickerSeconds)
        npMin?.let { picker ->
            picker.minValue = 0
            picker.maxValue = 120
            picker.value = this.minutes
        }
        npSec?.let { picker ->
            picker.minValue = 0
            picker.maxValue = 59
            picker.value = this.seconds
        }
        return create
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.activity = context as Activity
    }
}
