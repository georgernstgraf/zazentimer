package at.priv.graf.zazentimer.fragments

import androidx.annotation.NonNull
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
        @NonNull recyclerView: RecyclerView,
        @NonNull viewHolder: RecyclerView.ViewHolder,
    ): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        val swipeFlags = ItemTouchHelper.START
        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onMove(
        @NonNull recyclerView: RecyclerView,
        @NonNull viewHolder: RecyclerView.ViewHolder,
        @NonNull target: RecyclerView.ViewHolder,
    ): Boolean = listener?.onMove(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition) ?: false

    override fun onSwiped(
        @NonNull viewHolder: RecyclerView.ViewHolder,
        direction: Int,
    ) {
        listener?.onSwipe(viewHolder.bindingAdapterPosition)
    }

    override fun isLongPressDragEnabled(): Boolean = true

    override fun isItemViewSwipeEnabled(): Boolean = true
}
