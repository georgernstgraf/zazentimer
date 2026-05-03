package at.priv.graf.fragments;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import at.priv.graf.zazentimer.R;
import at.priv.graf.zazentimer.bo.Section;
import java.util.ArrayList;
import java.util.List;

public class SectionListAdapter extends RecyclerView.Adapter<SectionListAdapter.ViewHolder> {

    private List<Section> items = new ArrayList<>();
    private final OnItemClickListener clickListener;
    private final OnSectionActionListener actionListener;

    public interface OnItemClickListener {
        void onItemClick(Section section);
    }

    public interface OnSectionActionListener {
        void onDeleteSection(int position);
        void onDuplicateSection(int position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView spinnerText1;
        final TextView spinnerText2;
        final ImageView dragHandle;
        final ImageButton sectionOverflow;

        ViewHolder(View view) {
            super(view);
            dragHandle = view.findViewById(R.id.dragHandle);
            spinnerText1 = view.findViewById(R.id.spinnerText1);
            spinnerText2 = view.findViewById(R.id.spinnerText2);
            sectionOverflow = view.findViewById(R.id.sectionOverflow);
        }
    }

    public SectionListAdapter(OnItemClickListener clickListener) {
        this(clickListener, null);
    }

    public SectionListAdapter(OnItemClickListener clickListener, OnSectionActionListener actionListener) {
        this.clickListener = clickListener;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.session_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Section section = items.get(position);

        String name;
        if (section.name != null && section.name.length() > 0) {
            name = section.name;
        } else {
            name = holder.itemView.getContext().getString(R.string.unnamed);
        }

        String durationStr = section.getDurationString() + ", ";
        String info;
        if (section.bellcount == 1) {
            info = durationStr + holder.itemView.getContext().getString(R.string.section_info_string_1_sg);
        } else {
            String partial = durationStr + String.format(holder.itemView.getContext().getString(R.string.section_info_string_1_pl), section.bellcount) + " ";
            if (section.bellpause == 1) {
                info = partial + holder.itemView.getContext().getString(R.string.section_info_string_2_sg);
            } else {
                info = partial + String.format(holder.itemView.getContext().getString(R.string.section_info_string_2_pl), section.bellpause);
            }
        }

        holder.spinnerText1.setText(name);
        holder.spinnerText2.setText(info);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onItemClick(items.get(holder.getBindingAdapterPosition()));
            }
        });

        holder.sectionOverflow.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            popup.getMenuInflater().inflate(R.menu.menu_section_card_actions, popup.getMenu());
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    int pos = holder.getBindingAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION || actionListener == null) {
                        return false;
                    }
                    int id = menuItem.getItemId();
                    if (id == R.id.card_action_delete_section) {
                        actionListener.onDeleteSection(pos);
                        return true;
                    } else if (id == R.id.card_action_duplicate_section) {
                        actionListener.onDuplicateSection(pos);
                        return true;
                    }
                    return false;
                }
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setItems(List<Section> newItems) {
        items = new ArrayList<>(newItems);
        notifyDataSetChanged();
    }

    public Section getItem(int position) {
        return items.get(position);
    }

    public Section removeItem(int position) {
        Section removed = items.remove(position);
        notifyItemRemoved(position);
        return removed;
    }

    public void insertItem(int position, Section section) {
        items.add(position, section);
        notifyItemInserted(position);
    }

    public List<Section> getItems() {
        return new ArrayList<>(items);
    }

    public void moveItem(int fromPosition, int toPosition) {
        Section item = items.remove(fromPosition);
        items.add(toPosition, item);
        notifyItemMoved(fromPosition, toPosition);
    }
}
