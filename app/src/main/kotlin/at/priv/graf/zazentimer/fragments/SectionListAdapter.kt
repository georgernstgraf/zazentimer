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
import at.priv.graf.zazentimer.bo.Section

class SectionListAdapter(
    private val clickListener: OnItemClickListener?,
    private val actionListener: OnSectionActionListener? = null,
) : RecyclerView.Adapter<SectionListAdapter.ViewHolder>() {
    interface OnItemClickListener {
        fun onItemClick(section: Section)
    }

    interface OnSectionActionListener {
        fun onDeleteSection(position: Int)

        fun onDuplicateSection(position: Int)
    }

    private var items: MutableList<Section> = ArrayList()

    class ViewHolder(
        view: View,
    ) : RecyclerView.ViewHolder(view) {
        val spinnerText1: TextView = view.findViewById(R.id.spinnerText1)
        val spinnerText2: TextView = view.findViewById(R.id.spinnerText2)
        val dragHandle: ImageView = view.findViewById(R.id.dragHandle)
        val sectionOverflow: ImageButton = view.findViewById(R.id.sectionOverflow)
    }

    @NonNull
    override fun onCreateViewHolder(
        @NonNull parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.session_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        @NonNull holder: ViewHolder,
        position: Int,
    ) {
        val section = items[position]

        val name: String =
            section.name?.takeIf { it.isNotEmpty() } ?: holder.itemView.context.getString(R.string.unnamed)

        val durationStr = "${section.getDurationString()}, "
        val info: String =
            if (section.bellcount == 1) {
                "$durationStr${holder.itemView.context.getString(R.string.section_info_string_1_sg)}"
            } else {
                val partial =
                    "$durationStr${String.format(holder.itemView.context.getString(R.string.section_info_string_1_pl), section.bellcount)} "
                if (section.bellpause == 1) {
                    "$partial${holder.itemView.context.getString(R.string.section_info_string_2_sg)}"
                } else {
                    "$partial${String.format(holder.itemView.context.getString(R.string.section_info_string_2_pl), section.bellpause)}"
                }
            }

        holder.spinnerText1.text = name
        holder.spinnerText2.text = info

        holder.itemView.setOnClickListener {
            clickListener?.onItemClick(items[holder.bindingAdapterPosition])
        }

        holder.sectionOverflow.setOnClickListener {
            val popup = PopupMenu(it.context, it)
            popup.menuInflater.inflate(R.menu.menu_section_card_actions, popup.menu)
            popup.setOnMenuItemClickListener(
                object : PopupMenu.OnMenuItemClickListener {
                    override fun onMenuItemClick(menuItem: MenuItem): Boolean {
                        val pos = holder.bindingAdapterPosition
                        if (pos == RecyclerView.NO_POSITION || actionListener == null) {
                            return false
                        }
                        val id = menuItem.itemId
                        if (id == R.id.card_action_delete_section) {
                            actionListener.onDeleteSection(pos)
                            return true
                        } else if (id == R.id.card_action_duplicate_section) {
                            actionListener.onDuplicateSection(pos)
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

    fun setItems(newItems: List<Section>) {
        items = ArrayList(newItems)
        notifyDataSetChanged()
    }

    fun getItem(position: Int): Section = items[position]

    fun removeItem(position: Int): Section {
        val removed = items.removeAt(position)
        notifyItemRemoved(position)
        return removed
    }

    fun insertItem(
        position: Int,
        section: Section,
    ) {
        items.add(position, section)
        notifyItemInserted(position)
    }

    fun getItems(): List<Section> = ArrayList(items)

    fun moveItem(
        fromPosition: Int,
        toPosition: Int,
    ) {
        val item = items.removeAt(fromPosition)
        items.add(toPosition, item)
        notifyItemMoved(fromPosition, toPosition)
    }
}
