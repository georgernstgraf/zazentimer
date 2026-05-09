package at.priv.graf.zazentimer.fragments

import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import at.priv.graf.zazentimer.R

class SessionListAdapter(
    private val clickListener: OnItemClickListener?,
    private val actionListener: OnSessionActionListener? = null,
) : RecyclerView.Adapter<SessionListAdapter.ViewHolder>() {
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
        val sessionName: TextView = view.findViewById(R.id.sessionName)
        val sessionDescription: TextView = view.findViewById(R.id.sessionDescription)
        val sessionDuration: TextView = view.findViewById(R.id.sessionDuration)
        val sessionOverflow: ImageButton = view.findViewById(R.id.sessionOverflow)
        val dragHandle: ImageView = view.findViewById(R.id.dragHandle)
    }

    @NonNull
    override fun onCreateViewHolder(
        @NonNull parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_session, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        @NonNull holder: ViewHolder,
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
            val previous = selectedPosition
            selectedPosition = holder.bindingAdapterPosition
            if (previous != -1) {
                notifyItemChanged(previous)
            }
            notifyItemChanged(selectedPosition)
            clickListener?.onItemClick(selectedPosition, items[selectedPosition])
        }

        holder.sessionOverflow.setOnClickListener {
            if (!interactionsEnabled) return@setOnClickListener
            val popup = PopupMenu(it.context, it)
            popup.menuInflater.inflate(R.menu.menu_session_card_actions, popup.menu)
            popup.setOnMenuItemClickListener(
                object : PopupMenu.OnMenuItemClickListener {
                    override fun onMenuItemClick(menuItem: MenuItem): Boolean {
                        val pos = holder.bindingAdapterPosition
                        if (pos == RecyclerView.NO_POSITION || actionListener == null) {
                            return false
                        }
                        val id = menuItem.itemId
                        if (id == R.id.card_action_edit) {
                            actionListener.onEditSession(pos)
                            return true
                        } else if (id == R.id.card_action_copy) {
                            actionListener.onCopySession(pos)
                            return true
                        } else if (id == R.id.card_action_delete) {
                            actionListener.onDeleteSession(pos)
                            return true
                        }
                        return false
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
        val previous = selectedPosition
        selectedPosition = position
        if (previous != -1) {
            notifyItemChanged(previous)
        }
        if (selectedPosition != -1) {
            notifyItemChanged(selectedPosition)
        }
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

    fun getSelectedPosition(): Int = selectedPosition

    fun getItem(position: Int): SessionWithTimeInfo? {
        if (position >= 0 && position < items.size) {
            return items[position]
        }
        return null
    }

    private fun formatDuration(totalSeconds: Int): String =
        String.format(java.util.Locale.getDefault(), "%02d:%02d", totalSeconds / 60, totalSeconds % 60)
}
