package de.gaffga.android.zazentimer;

import android.os.Handler;
import android.util.Log;
import android.view.View;
import de.gaffga.android.fragments.MeditationFragment;
import de.gaffga.android.zazentimer.service.Meditation;
import de.gaffga.android.zazentimer.service.MeditationServiceBinder;
import de.gaffga.android.zazentimer.views.TimerView;

public class MeditationUIUpdateThread implements Runnable {
    private static final String TAG = "ZMT_UIUpdateThread";
    private MeditationServiceBinder binder;
    private MeditationFragment fragment;
    private final Handler handler;
    private Boolean stop = false;
    private TimerView timerView;

    public MeditationUIUpdateThread(Handler handler, MeditationFragment meditationFragment, MeditationServiceBinder meditationServiceBinder) {
        this.handler = handler;
        this.fragment = meditationFragment;
        this.binder = meditationServiceBinder;
    }

    @Override // java.lang.Runnable
    public void run() {
        if (this.stop.booleanValue()) {
            return;
        }
        update();
        this.handler.postDelayed(this, 300L);
    }

    private void update() {
        Meditation runningMeditation;
        View view = this.fragment.getView();
        if (view == null || (runningMeditation = this.binder.getService().getRunningMeditation()) == null) {
            return;
        }
        if (this.timerView == null) {
            this.timerView = (TimerView) view.findViewById(R.id.timerView);
            this.timerView.setCurrentStartSeconds(0);
            this.timerView.setNumTotalSeconds(runningMeditation.getTotalSessionTime());
            this.timerView.setNextEndSeconds(runningMeditation.getNextEndSeconds());
            this.timerView.setNextStartSeconds(runningMeditation.getNextStartSeconds());
            this.timerView.setPrevStartSeconds(runningMeditation.getPrevStartSeconds());
            this.timerView.setSessionElapsedSeconds(0);
            this.timerView.setSectionElapsedSeconds(0);
            this.timerView.setSectionNamesNoAnim(runningMeditation.getCurrentSectionName(), runningMeditation.getNextSectionName());
        }
        if (runningMeditation.getCurrentSection() != null) {
            this.timerView.setCurrentStartSeconds(runningMeditation.getCurrentStartSeconds());
            this.timerView.setNumTotalSeconds(runningMeditation.getTotalSessionTime());
            this.timerView.setNextEndSeconds(runningMeditation.getNextEndSeconds());
            this.timerView.setNextStartSeconds(runningMeditation.getNextStartSeconds());
            this.timerView.setPrevStartSeconds(runningMeditation.getPrevStartSeconds());
            this.timerView.setSectionElapsedSeconds(runningMeditation.getSectionElapsedSeconds());
            this.timerView.setSessionElapsedSeconds(runningMeditation.getCurrentSessionTime());
            this.timerView.setSectionNames(runningMeditation.getCurrentSectionName(), runningMeditation.getNextSectionName(), runningMeditation.getNextNextSectionName());
        }
    }

    public void stopUpdates() {
        Log.d(TAG, "stopping UI updates");
        this.stop = true;
    }
}
