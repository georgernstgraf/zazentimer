package de.gaffga.android.fragments;

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
import de.gaffga.android.zazentimer.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SessionListAdapter extends RecyclerView.Adapter<SessionListAdapter.ViewHolder> {

    private List<SessionWithTimeInfo> items = new ArrayList<>();
    private int selectedPosition = -1;
    private boolean interactionsEnabled = true;
    private final OnItemClickListener clickListener;
    private final OnSessionActionListener actionListener;

    public interface OnItemClickListener {
        void onItemClick(int position, SessionWithTimeInfo session);
    }

    public interface OnSessionActionListener {
        void onEditSession(int position);
        void onCopySession(int position);
        void onDeleteSession(int position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView sessionName;
        final TextView sessionDescription;
        final TextView sessionDuration;
        final ImageButton sessionOverflow;
        final ImageView dragHandle;

        ViewHolder(View view) {
            super(view);
            sessionName = view.findViewById(R.id.sessionName);
            sessionDescription = view.findViewById(R.id.sessionDescription);
            sessionDuration = view.findViewById(R.id.sessionDuration);
            sessionOverflow = view.findViewById(R.id.sessionOverflow);
            dragHandle = view.findViewById(R.id.dragHandle);
        }
    }

    public SessionListAdapter(OnItemClickListener clickListener) {
        this(clickListener, null);
    }

    public SessionListAdapter(OnItemClickListener clickListener, OnSessionActionListener actionListener) {
        this.clickListener = clickListener;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_session, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SessionWithTimeInfo item = items.get(position);

        String name;
        if (item.getSession() != null && item.getSession().name != null && !item.getSession().name.trim().equals("")) {
            name = item.getSession().name;
        } else {
            name = holder.itemView.getContext().getString(R.string.session_list_unnamed_entry);
        }

        holder.sessionName.setText(name);
        holder.sessionDuration.setText(formatDuration(item.getTotalTimeSeconds()));

        if (item.getSession() != null && item.getSession().description != null) {
            holder.sessionDescription.setText(item.getSession().description);
        } else {
            holder.sessionDescription.setText("");
        }

        holder.itemView.setSelected(position == selectedPosition);
        holder.itemView.setActivated(position == selectedPosition);

        holder.itemView.setOnClickListener(v -> {
            if (!interactionsEnabled) return;
            int previous = selectedPosition;
            selectedPosition = holder.getBindingAdapterPosition();
            if (previous != -1) {
                notifyItemChanged(previous);
            }
            notifyItemChanged(selectedPosition);
            if (clickListener != null) {
                clickListener.onItemClick(selectedPosition, items.get(selectedPosition));
            }
        });

        holder.sessionOverflow.setOnClickListener(v -> {
            if (!interactionsEnabled) return;
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            popup.getMenuInflater().inflate(R.menu.menu_session_card_actions, popup.getMenu());
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    int pos = holder.getBindingAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION || actionListener == null) {
                        return false;
                    }
                    int id = menuItem.getItemId();
                    if (id == R.id.card_action_edit) {
                        actionListener.onEditSession(pos);
                        return true;
                    } else if (id == R.id.card_action_copy) {
                        actionListener.onCopySession(pos);
                        return true;
                    } else if (id == R.id.card_action_delete) {
                        actionListener.onDeleteSession(pos);
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

    public void setSessions(ArrayList<SessionWithTimeInfo> newItems) {
        items = new ArrayList<>(newItems);
        selectedPosition = -1;
        notifyDataSetChanged();
    }

    public void setSelectedPosition(int position) {
        int previous = selectedPosition;
        selectedPosition = position;
        if (previous != -1) {
            notifyItemChanged(previous);
        }
        if (selectedPosition != -1) {
            notifyItemChanged(selectedPosition);
        }
    }

    public void setInteractionsEnabled(boolean enabled) {
        this.interactionsEnabled = enabled;
    }

    public void moveItem(int fromPosition, int toPosition) {
        SessionWithTimeInfo moved = items.remove(fromPosition);
        items.add(toPosition, moved);
        notifyItemMoved(fromPosition, toPosition);
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public SessionWithTimeInfo getItem(int position) {
        if (position >= 0 && position < items.size()) {
            return items.get(position);
        }
        return null;
    }

    private String formatDuration(int totalSeconds) {
        return String.format(Locale.getDefault(), "%02d:%02d", totalSeconds / 60, totalSeconds % 60);
    }
}
