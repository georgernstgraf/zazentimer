package at.priv.graf.zazentimer;

import android.os.Handler;

public class RunOnConnect {
    private final Handler handler;
    private final Runnable runOnConnect;

    public RunOnConnect(Handler handler, Runnable runnable) {
        this.handler = handler;
        this.runOnConnect = runnable;
    }

    public Handler getHandler() {
        return this.handler;
    }

    public Runnable getRunOnConnect() {
        return this.runOnConnect;
    }
}
