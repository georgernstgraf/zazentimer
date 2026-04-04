package de.gaffga.android.fragments;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import de.gaffga.android.zazentimer.R;
import java.util.ArrayList;
import java.util.Locale;

public class SessionListAdapter extends ArrayAdapter<SessionWithTimeInfo> {
    private final Context context;

    public SessionListAdapter(Context context, int i, int i2) {
        super(context, i, i2);
        this.context = context;
    }

    public void setSessions(ArrayList<SessionWithTimeInfo> arrayList) {
        clear();
        addAll(arrayList);
        notifyDataSetChanged();
    }

    @Override // android.widget.ArrayAdapter, android.widget.Adapter
    public View getView(int i, View view, ViewGroup viewGroup) {
        return getMyView(i, R.layout.main_session_list_item);
    }

    private View getMyView(int i, int i2) {
        View inflate = LayoutInflater.from(this.context).inflate(i2, (ViewGroup) null);
        TextView textView = (TextView) inflate.findViewById(R.id.spinnerText1);
        TextView textView2 = (TextView) inflate.findViewById(R.id.spinnerText2);
        TextView textView3 = (TextView) inflate.findViewById(R.id.spinnerText3);
        SessionWithTimeInfo item = getItem(i);
        if (item != null) {
            textView3.setText(getSessionTimeInfo(item.getTotalTimeSeconds()));
            if (item.getSession() != null) {
                if (item.getSession().name.trim().equals("")) {
                    textView.setText(this.context.getString(R.string.session_list_unnamed_entry));
                } else {
                    textView.setText(item.getSession().name);
                }
                textView2.setText(item.getSession().description);
            } else {
                textView.setText("" + i);
                textView2.setText("");
            }
        } else {
            textView.setText("" + i);
            textView2.setText("");
            textView3.setText(0);
        }
        return inflate;
    }

    private String getSessionTimeInfo(int i) {
        return String.format(Locale.getDefault(), "%02d:%02d", Integer.valueOf(i / 60), Integer.valueOf(i % 60));
    }

    @Override // android.widget.ArrayAdapter, android.widget.BaseAdapter, android.widget.SpinnerAdapter
    public View getDropDownView(int i, View view, ViewGroup viewGroup) {
        return getMyView(i, R.layout.main_session_list_item);
    }
}
