package at.priv.graf.fragments;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import at.priv.graf.zazentimer.bo.Bell;
import at.priv.graf.zazentimer.R;

public class GongListAdapter extends ArrayAdapter<Bell> {
    private final Context context;

    public GongListAdapter(Context context, int i, int i2) {
        super(context, i, i2);
        this.context = context;
    }

    @Override // android.widget.ArrayAdapter, android.widget.Adapter
    public View getView(int i, View view, ViewGroup viewGroup) {
        return getMyView(i, R.layout.spinner_single_item);
    }

    private View getMyView(int i, int i2) {
        View inflate = LayoutInflater.from(this.context).inflate(i2, (ViewGroup) null);
        ((TextView) inflate.findViewById(R.id.spinnerText1)).setText(getItem(i).getName());
        return inflate;
    }

    @Override // android.widget.ArrayAdapter, android.widget.BaseAdapter, android.widget.SpinnerAdapter
    public View getDropDownView(int i, View view, ViewGroup viewGroup) {
        return getMyView(i, R.layout.spinner_popup_single_item);
    }
}
