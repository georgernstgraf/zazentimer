package de.gaffga.android.fragments;

import android.app.Activity;
import androidx.fragment.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import de.gaffga.android.zazentimer.R;

/* loaded from: classes.dex */
public class MeditationFragment extends androidx.fragment.app.Fragment {
    private static final String TAG = "ZMT_MeditationFragment";
    private ImageButton butPause;
    private ImageButton butStop;
    private Context context;
    private boolean mAttached = false;
    private OnFragmentInteractionListener mListener;

    /* loaded from: classes.dex */
    public interface OnFragmentInteractionListener {
        boolean isPaused();

        void onPauseClicked();

        void onStopClicked();
    }

    @Override // android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    @Override // android.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View inflate = layoutInflater.inflate(R.layout.fragment_meditation, viewGroup, false);
        this.butStop = (ImageButton) inflate.findViewById(R.id.but_stop);
        this.butPause = (ImageButton) inflate.findViewById(R.id.but_pause);
        this.butStop.setOnClickListener(new View.OnClickListener() { // from class: de.gaffga.android.fragments.MeditationFragment.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                if (MeditationFragment.this.mListener != null) {
                    MeditationFragment.this.mListener.onStopClicked();
                }
                MeditationFragment.this.updateButtons();
            }
        });
        this.butPause.setOnClickListener(new View.OnClickListener() { // from class: de.gaffga.android.fragments.MeditationFragment.2
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                if (MeditationFragment.this.mListener != null) {
                    MeditationFragment.this.mListener.onPauseClicked();
                }
                MeditationFragment.this.updateButtons();
            }
        });
        updateButtons();
        return inflate;
    }

    @Override // android.app.Fragment
    public void onResume() {
        super.onResume();
        getActivity().invalidateOptionsMenu();
    }

    @Override // android.app.Fragment
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(TAG, "onAttach (Activity)");
        handleAttach(activity);
    }

    @Override // android.app.Fragment
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(TAG, "onAttach (Context)");
        handleAttach(context);
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void handleAttach(Context context) {
        if (context == null) {
            return;
        }
        this.context = context;
        this.mAttached = true;
        if (context instanceof OnFragmentInteractionListener) {
            this.mListener = (OnFragmentInteractionListener) context;
            return;
        }
        throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
    }

    @Override // android.app.Fragment
    public void onDetach() {
        Log.d(TAG, "onDetach");
        super.onDetach();
        this.mListener = null;
        this.mAttached = false;
    }

    public void updateButtons() {
        if (this.mListener != null && this.mListener.isPaused()) {
            this.butPause.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_play_arrow_white_48dp));
        } else {
            this.butPause.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_pause_white_48dp));
        }
    }
}
