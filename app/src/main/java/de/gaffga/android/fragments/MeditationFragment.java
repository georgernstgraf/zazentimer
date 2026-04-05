package de.gaffga.android.fragments;

import android.app.Activity;
import androidx.fragment.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import de.gaffga.android.zazentimer.R;
import de.gaffga.android.zazentimer.service.MeditationUiState;
import de.gaffga.android.zazentimer.service.MeditationViewModel;
import de.gaffga.android.zazentimer.views.TimerView;

public class MeditationFragment extends androidx.fragment.app.Fragment {
    private static final String TAG = "ZMT_MeditationFragment";
    private ImageButton butPause;
    private ImageButton butStop;
    private Context context;
    private boolean mAttached = false;
    private MeditationViewModel viewModel;

    public MeditationFragment() {
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View inflate = layoutInflater.inflate(R.layout.fragment_meditation, viewGroup, false);
        this.butStop = (ImageButton) inflate.findViewById(R.id.but_stop);
        this.butPause = (ImageButton) inflate.findViewById(R.id.but_pause);
        this.butStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (MeditationFragment.this.viewModel != null) {
                    MeditationFragment.this.viewModel.stopMeditation();
                }
                MeditationFragment.this.updateButtons();
            }
        });
        this.butPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (MeditationFragment.this.viewModel != null) {
                    MeditationFragment.this.viewModel.pauseMeditation();
                }
                MeditationFragment.this.updateButtons();
            }
        });
        updateButtons();
        return inflate;
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(TAG, "onAttach (Activity)");
        handleAttach(activity);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(TAG, "onAttach (Context)");
        handleAttach(context);
    }

    private void handleAttach(Context context) {
        if (context == null) {
            return;
        }
        this.context = context;
        this.mAttached = true;
    }

    @Override
    public void onDetach() {
        Log.d(TAG, "onDetach");
        super.onDetach();
        this.mAttached = false;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MeditationViewModel.class);
        viewModel.getMeditationState().observe(getViewLifecycleOwner(), state -> {
            if (state == null || !state.running) {
                return;
            }
            TimerView timerView = (TimerView) view.findViewById(R.id.timerView);
            if (timerView != null) {
                timerView.setCurrentStartSeconds(state.currentStartSeconds);
                timerView.setNumTotalSeconds(state.totalSessionTime);
                timerView.setNextEndSeconds(state.nextEndSeconds);
                timerView.setNextStartSeconds(state.nextStartSeconds);
                timerView.setPrevStartSeconds(state.prevStartSeconds);
                timerView.setSectionElapsedSeconds(state.sectionElapsedSeconds);
                timerView.setSessionElapsedSeconds(state.sessionElapsedSeconds);
                timerView.setSectionNames(state.currentSectionName, state.nextSectionName, state.nextNextSectionName);
            }
            updateButtons();
        });
    }

    public void updateButtons() {
        if (this.butPause == null || getActivity() == null) {
            return;
        }
        if (this.viewModel != null && this.viewModel.isPaused()) {
            this.butPause.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_play_arrow_white_48dp));
        } else {
            this.butPause.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_pause_white_48dp));
        }
    }
}
