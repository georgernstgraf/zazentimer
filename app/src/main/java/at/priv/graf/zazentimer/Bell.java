package at.priv.graf.zazentimer;

import android.net.Uri;

public class Bell {
    private String name;
    private Uri uri;

    public Bell(Uri uri, String str) {
        this.uri = uri;
        this.name = str;
    }

    public String getName() {
        if (this.name.startsWith("bell_")) {
            return this.name.substring(5);
        }
        return this.name;
    }

    public Uri getUri() {
        return this.uri;
    }
}
