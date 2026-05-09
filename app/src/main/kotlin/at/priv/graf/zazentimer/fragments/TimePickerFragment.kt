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

    fun getSeconds(): Int = this.npSec!!.value

    fun setSeconds(i: Int) {
        this.seconds = i
        this.npSec?.setValue(i)
    }

    fun getMinutes(): Int = this.npMin!!.value

    fun setMinutes(i: Int) {
        this.minutes = i
        this.npMin?.setValue(i)
    }

    override fun onCreateDialog(bundle: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        this.view = this.activity!!.layoutInflater.inflate(R.layout.dialog_time_picker, null as ViewGroup?)
        builder.setView(this.view)
        builder.setPositiveButton(R.string.ok) { _: DialogInterface, _: Int ->
            this@TimePickerFragment.seconds = this@TimePickerFragment.npSec!!.value
            this@TimePickerFragment.minutes = this@TimePickerFragment.npMin!!.value
            this@TimePickerFragment.onOkRunnable?.run()
        }
        builder.setNegativeButton(R.string.abbrechen) { _: DialogInterface, _: Int ->
        }
        val create = builder.create()
        this.npMin = this.view!!.findViewById(R.id.pickerMinutes)
        this.npSec = this.view!!.findViewById(R.id.pickerSeconds)
        this.npMin!!.minValue = 0
        this.npMin!!.maxValue = 120
        this.npMin!!.value = this.minutes
        this.npSec!!.minValue = 0
        this.npSec!!.maxValue = 59
        this.npSec!!.value = this.seconds
        return create
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.activity = context as Activity
    }
}
