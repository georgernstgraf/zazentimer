package at.priv.graf.zazentimer.utils;

import androidx.test.espresso.IdlingResource;

import java.util.concurrent.atomic.AtomicBoolean;

import at.priv.graf.zazentimer.database.DbOperations;

public class DatabaseIdlingResource implements IdlingResource {

    private final DbOperations dbOperations;
    private volatile ResourceCallback resourceCallback;
    private final AtomicBoolean isIdle = new AtomicBoolean(true);

    public DatabaseIdlingResource(DbOperations dbOperations) {
        this.dbOperations = dbOperations;
    }

    @Override
    public String getName() {
        return "DatabaseIdlingResource";
    }

    @Override
    public boolean isIdleNow() {
        return isIdle.get();
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback callback) {
        this.resourceCallback = callback;
        notifyIdle();
    }

    public void setBusy() {
        isIdle.set(false);
    }

    public void setIdle() {
        isIdle.set(true);
        notifyIdle();
    }

    private void notifyIdle() {
        if (resourceCallback != null) {
            resourceCallback.onTransitionToIdle();
        }
    }

    public <T> T wrap(RunnableQuery<T> query) {
        setBusy();
        try {
            return query.run();
        } finally {
            setIdle();
        }
    }

    public void wrap(RunnableVoidQuery query) {
        setBusy();
        try {
            query.run();
        } finally {
            setIdle();
        }
    }

    public interface RunnableQuery<T> {
        T run();
    }

    public interface RunnableVoidQuery {
        void run();
    }
}
