package de.gaffga.android.fragments;

import androidx.activity.OnBackPressedCallback;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.gaffga.android.zazentimer.R;
import de.gaffga.android.zazentimer.service.MeditationUiState;
import de.gaffga.android.zazentimer.service.MeditationViewModel;
import de.gaffga.android.zazentimer.views.TimerView;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MeditationFragment extends Fragment {
    private static final String TAG = "ZMT_MeditationFragment";
    private ImageButton butPause;
    private ImageButton butStop;
    private Context context;
    private boolean mAttached = false;
    private MeditationViewModel viewModel;
    private boolean meditationRunning = false;
    private OnBackPressedCallback backPressedCallback;

    public MeditationFragment() {
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
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(TAG, "onAttach (Context)");
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

        backPressedCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                showStopConfirmationDialog();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backPressedCallback);

        viewModel.getMeditationState().observe(getViewLifecycleOwner(), state -> {
            if (state == null || !state.running) {
                meditationRunning = false;
                if (backPressedCallback != null) {
                    backPressedCallback.setEnabled(false);
                }
                return;
            }
            meditationRunning = true;
            if (backPressedCallback != null) {
                backPressedCallback.setEnabled(true);
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

    private void showStopConfirmationDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.stop_meditation_title)
                .setMessage(R.string.stop_meditation_message)
                .setPositiveButton(R.string.stop_meditation_stop, (dialog, which) -> {
                    if (viewModel != null) {
                        viewModel.stopMeditation();
                    }
                    backPressedCallback.setEnabled(false);
                    requireActivity().onBackPressed();
                })
                .setNegativeButton(R.string.stop_meditation_cancel, (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
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
