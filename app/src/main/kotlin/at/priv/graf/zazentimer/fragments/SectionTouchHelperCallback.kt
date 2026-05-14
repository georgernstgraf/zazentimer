package at.priv.graf.zazentimer.fragments

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class SectionTouchHelperCallback(
    private val listener: SectionTouchListener?,
) : ItemTouchHelper.Callback() {
    interface SectionTouchListener {
        fun onSwipe(position: Int)

        fun onMove(
            fromPosition: Int,
            toPosition: Int,
        ): Boolean
    }

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
    ): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        val swipeFlags = ItemTouchHelper.START
        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder,
    ): Boolean = listener?.onMove(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition) ?: false

    override fun onSwiped(
        viewHolder: RecyclerView.ViewHolder,
        direction: Int,
    ) {
        listener?.onSwipe(viewHolder.bindingAdapterPosition)
    }

    override fun isLongPressDragEnabled(): Boolean = true

    override fun isItemViewSwipeEnabled(): Boolean = true
}
