package at.priv.graf.zazentimer.fragments

import android.view.LayoutInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import at.priv.graf.zazentimer.R
import at.priv.graf.zazentimer.bo.TimeFormat

@Suppress("TooManyFunctions")
class SessionListAdapter(
    private val clickListener: OnItemClickListener?,
    private val actionListener: OnSessionActionListener? = null,
) : RecyclerView.Adapter<SessionListAdapter.ViewHolder>() {
    var onDragHandleTouched: ((RecyclerView.ViewHolder) -> Unit)? = null

    interface OnItemClickListener {
        fun onItemClick(
            position: Int,
            session: SessionWithTimeInfo,
        )
    }

    interface OnSessionActionListener {
        fun onEditSession(position: Int)

        fun onCopySession(position: Int)

        fun onDeleteSession(position: Int)
    }

    private var items: MutableList<SessionWithTimeInfo> = ArrayList()
    private var selectedPosition: Int = -1
    private var interactionsEnabled: Boolean = true

    class ViewHolder(
        view: View,
    ) : RecyclerView.ViewHolder(view) {
        val sessionName: TextView = view.findViewById(R.id.session_name)
        val sessionDescription: TextView = view.findViewById(R.id.session_description)
        val sessionDuration: TextView = view.findViewById(R.id.session_duration)
        val sessionOverflow: ImageButton = view.findViewById(R.id.session_overflow)
        val dragHandle: ImageView = view.findViewById(R.id.drag_handle)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_session, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val item = items[position]

        val sessionName = item.session.name
        val name: String =
            if (sessionName != null && sessionName.trim() != "") {
                sessionName
            } else {
                holder.itemView.context.getString(R.string.session_list_unnamed_entry)
            }

        holder.sessionName.text = name
        holder.sessionDuration.text = formatDuration(item.totalTimeSeconds)

        val desc = item.session.description ?: ""
        if (desc.trim().isEmpty()) {
            holder.sessionDescription.visibility = View.GONE
        } else {
            holder.sessionDescription.visibility = View.VISIBLE
            holder.sessionDescription.text = desc
        }

        holder.itemView.isSelected = position == selectedPosition
        holder.itemView.isActivated = position == selectedPosition

        holder.itemView.setOnClickListener {
            if (!interactionsEnabled) return@setOnClickListener
            selectedPosition = holder.bindingAdapterPosition
            for (i in items.indices) notifyItemChanged(i)
            clickListener?.onItemClick(selectedPosition, items[selectedPosition])
        }

        holder.itemView.setOnLongClickListener {
            if (!interactionsEnabled) return@setOnLongClickListener false
            actionListener?.onEditSession(holder.bindingAdapterPosition)
            true
        }

        holder.dragHandle.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                onDragHandleTouched?.invoke(holder)
            }
            false
        }

        holder.sessionOverflow.setOnClickListener {
            if (!interactionsEnabled) return@setOnClickListener
            val popup = PopupMenu(it.context, it)
            popup.menuInflater.inflate(R.menu.menu_session_card_actions, popup.menu)
            popup.setOnMenuItemClickListener(
                object : PopupMenu.OnMenuItemClickListener {
                    override fun onMenuItemClick(menuItem: MenuItem): Boolean {
                        val pos = holder.bindingAdapterPosition
                        if (pos == RecyclerView.NO_POSITION || actionListener == null) return false
                        return when (menuItem.itemId) {
                            R.id.card_action_edit -> {
                                actionListener.onEditSession(pos)
                                true
                            }
                            R.id.card_action_copy -> {
                                actionListener.onCopySession(pos)
                                true
                            }
                            R.id.card_action_delete -> {
                                actionListener.onDeleteSession(pos)
                                true
                            }
                            else -> false
                        }
                    }
                },
            )
            popup.show()
        }
    }

    override fun getItemCount(): Int = items.size

    fun setSessions(newItems: ArrayList<SessionWithTimeInfo>) {
        items = ArrayList(newItems)
        selectedPosition = -1
        notifyDataSetChanged()
    }

    fun setSelectedPosition(position: Int) {
        selectedPosition = position
        for (i in items.indices) notifyItemChanged(i)
    }

    fun setInteractionsEnabled(enabled: Boolean) {
        this.interactionsEnabled = enabled
    }

    fun moveItem(
        fromPosition: Int,
        toPosition: Int,
    ) {
        val moved = items.removeAt(fromPosition)
        items.add(toPosition, moved)
        notifyItemMoved(fromPosition, toPosition)
    }

    fun removeItem(position: Int): SessionWithTimeInfo {
        val removed = items.removeAt(position)
        notifyItemRemoved(position)
        return removed
    }

    fun insertItem(
        position: Int,
        item: SessionWithTimeInfo,
    ) {
        items.add(position, item)
        notifyItemInserted(position)
    }

    fun getItem(position: Int): SessionWithTimeInfo? {
        if (position >= 0 && position < items.size) {
            return items[position]
        }
        return null
    }

    private fun formatDuration(totalSeconds: Int): String = TimeFormat.mmss(totalSeconds)
}
