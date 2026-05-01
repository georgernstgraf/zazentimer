package de.gaffga.android.zazentimer.utils;

import androidx.test.espresso.IdlingResource;

public class MeditationServiceIdlingResource implements IdlingResource {

    private volatile ResourceCallback resourceCallback;
    private volatile boolean isIdle = true;

    @Override
    public String getName() {
        return "MeditationServiceIdlingResource";
    }

    @Override
    public boolean isIdleNow() {
        return isIdle;
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback callback) {
        this.resourceCallback = callback;
        callback.onTransitionToIdle();
    }

    public void setBusy() {
        isIdle = false;
    }

    public void setIdle() {
        isIdle = true;
        notifyIdle();
    }

    private void notifyIdle() {
        if (resourceCallback != null) {
            resourceCallback.onTransitionToIdle();
        }
    }
}
