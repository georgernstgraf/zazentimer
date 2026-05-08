package at.priv.graf.zazentimer.fragments

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView

class MaxHeightRecyclerView : RecyclerView {

    private var maxHeightPx: Int = Integer.MAX_VALUE

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    fun setMaxHeight(maxHeightPx: Int) {
        this.maxHeightPx = maxHeightPx
        requestLayout()
    }

    fun getMaxHeight(): Int = maxHeightPx

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        super.onMeasure(widthSpec, heightSpec)
        if (getMeasuredHeight() > maxHeightPx) {
            setMeasuredDimension(getMeasuredWidth(), maxHeightPx)
        }
    }
}
