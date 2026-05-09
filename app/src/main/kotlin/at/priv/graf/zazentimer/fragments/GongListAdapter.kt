package at.priv.graf.zazentimer.fragments

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import at.priv.graf.zazentimer.R
import at.priv.graf.zazentimer.bo.Bell

class GongListAdapter(
    context: Context,
    i: Int,
    i2: Int,
) : ArrayAdapter<Bell>(context, i, i2) {
    private val context: Context = context

    override fun getView(
        i: Int,
        view: View?,
        viewGroup: ViewGroup,
    ): View = getMyView(i, R.layout.spinner_single_item)

    private fun getMyView(
        i: Int,
        i2: Int,
    ): View {
        val inflate = LayoutInflater.from(this.context).inflate(i2, null as ViewGroup?)
        (inflate.findViewById<TextView>(R.id.spinnerText1)).text = getItem(i)?.getName() ?: ""
        return inflate
    }

    override fun getDropDownView(
        i: Int,
        view: View?,
        viewGroup: ViewGroup,
    ): View = getMyView(i, R.layout.spinner_popup_single_item)
}
