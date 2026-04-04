package de.gaffga.android.fragments;

import androidx.fragment.app.Fragment;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import de.gaffga.android.zazentimer.R;

/* loaded from: classes.dex */
public class AboutFragment extends androidx.fragment.app.Fragment {
    @Override // android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    @Override // android.app.Fragment
    public void onResume() {
        super.onResume();
        getActivity().invalidateOptionsMenu();
    }

    @Override // android.app.Fragment
    @Nullable
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View inflate = layoutInflater.inflate(R.layout.fragment_about, viewGroup, false);
        ((Button) inflate.findViewById(R.id.but_about_ok)).setOnClickListener(new View.OnClickListener() { // from class: de.gaffga.android.fragments.AboutFragment.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
        String str = "App-Version: ??";
        try {
            str = "App-Version: " + getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        ((TextView) inflate.findViewById(R.id.version)).setText(str);
        return inflate;
    }
}
