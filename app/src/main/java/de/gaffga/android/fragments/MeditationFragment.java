package de.gaffga.android.fragments;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.app.AlertDialog;
import com.google.android.material.transition.MaterialSharedAxis;
import de.gaffga.android.zazentimer.DbOperations;
import de.gaffga.android.zazentimer.R;
import de.gaffga.android.zazentimer.ZazenTimerActivity;
import de.gaffga.android.zazentimer.bo.Section;
import de.gaffga.android.zazentimer.bo.Session;
import de.gaffga.android.zazentimer.service.MeditationService;
import de.gaffga.android.zazentimer.service.MeditationUiState;
import de.gaffga.android.zazentimer.service.MeditationViewModel;
import de.gaffga.android.zazentimer.views.TimerView;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

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
    private TimerView timerView;

    @Inject
    DbOperations dbOperations;

    public MeditationFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.Y, true));
        setReturnTransition(new MaterialSharedAxis(MaterialSharedAxis.Y, false));
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View inflate = layoutInflater.inflate(R.layout.fragment_meditation, viewGroup, false);
        this.butStop = (ImageButton) inflate.findViewById(R.id.but_stop);
        this.butPause = (ImageButton) inflate.findViewById(R.id.but_pause);
        this.timerView = (TimerView) inflate.findViewById(R.id.timerView);
        this.butStop.setOnClickListener(view -> MeditationFragment.this.showStopConfirmationDialog());
        this.butPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (MeditationFragment.this.meditationRunning) {
                    if (MeditationFragment.this.viewModel != null) {
                        MeditationFragment.this.viewModel.pauseMeditation();
                    }
                    MeditationFragment.this.updateButtons();
                } else {
                    MeditationFragment.this.startMeditationFromIdle();
                }
            }
        });
        if (!MeditationService.isServiceRunning()) {
            showIdleState();
        } else {
            showRunningState();
            updateButtons();
        }
        return inflate;
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().invalidateOptionsMenu();
        if (!MeditationService.isServiceRunning()) {
            showIdleState();
        }
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
                showIdleState();
                return;
            }
            meditationRunning = true;
            if (backPressedCallback != null) {
                backPressedCallback.setEnabled(true);
            }
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
            showRunningState();
            updateButtons();
        });
    }

    private void showIdleState() {
        this.meditationRunning = false;
        if (timerView != null) {
            timerView.setCurrentStartSeconds(0);
            timerView.setNextEndSeconds(0);
            timerView.setNextStartSeconds(0);
            timerView.setPrevStartSeconds(0);
            timerView.setSectionElapsedSeconds(0);
            timerView.setSessionElapsedSeconds(0);
            SharedPreferences prefs = ZazenTimerActivity.getPreferences(requireContext());
            int sessionId = prefs.getInt(ZazenTimerActivity.PREF_KEY_LAST_SESSION, -1);
            if (sessionId != -1 && dbOperations != null) {
                Session session = dbOperations.readSession(sessionId);
                if (session != null) {
                    Section[] sections = dbOperations.readSections(sessionId);
                    int totalSeconds = 0;
                    for (Section s : sections) {
                        totalSeconds += s.duration;
                    }
                    timerView.setNumTotalSeconds(totalSeconds);
                    timerView.setSectionNamesNoAnim(session.name, "");
                } else {
                    timerView.setNumTotalSeconds(0);
                    timerView.setSectionNamesNoAnim("", "");
                }
            } else {
                timerView.setNumTotalSeconds(0);
                timerView.setSectionNamesNoAnim("", "");
            }
        }
        if (butStop != null) {
            butStop.setVisibility(View.GONE);
        }
        if (butPause != null) {
            butPause.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_play_arrow_white_48dp));
        }
    }

    private void showRunningState() {
        if (butStop != null) {
            butStop.setVisibility(View.VISIBLE);
        }
    }

    private void startMeditationFromIdle() {
        SharedPreferences prefs = ZazenTimerActivity.getPreferences(requireContext());
        int sessionId = prefs.getInt(ZazenTimerActivity.PREF_KEY_LAST_SESSION, -1);
        if (sessionId == -1) {
            return;
        }
        if (dbOperations.readSections(sessionId).length == 0) {
            return;
        }
        viewModel.setSelectedSessionId(sessionId);
        ZazenTimerActivity activity = (ZazenTimerActivity) getActivity();
        if (activity != null) {
            activity.startMeditation();
        }
    }

    private void showStopConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
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
        if (!meditationRunning) {
            this.butPause.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_play_arrow_white_48dp));
            return;
        }
        if (this.viewModel != null && this.viewModel.isPaused()) {
            this.butPause.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_play_arrow_white_48dp));
        } else {
            this.butPause.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_pause_white_48dp));
        }
    }
}
