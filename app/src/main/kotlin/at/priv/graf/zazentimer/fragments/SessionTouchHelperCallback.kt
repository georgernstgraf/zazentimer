package at.priv.graf.zazentimer.fragments

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class SessionTouchHelperCallback(
    private val listener: SessionTouchListener?,
) : ItemTouchHelper.Callback() {
    interface SessionTouchListener {
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
        return makeMovementFlags(dragFlags, 0)
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
        // no-op: drag-only mode
    }

    override fun isLongPressDragEnabled(): Boolean = true

    override fun isItemViewSwipeEnabled(): Boolean = false
}
