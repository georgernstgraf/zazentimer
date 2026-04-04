package de.gaffga.android.zazentimer.audio;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.Nullable;
import de.gaffga.android.zazentimer.Bell;
import de.gaffga.android.zazentimer.R;
import de.gaffga.android.zazentimer.bo.Section;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

public class BellCollection {
    public static int BELL_IDX_HIGH_TONE = 0;
    public static int BELL_IDX_JAP_RHINBOWL_107 = 2;
    public static int BELL_IDX_JAP_RHINBOWL_164 = 5;
    public static int BELL_IDX_JAP_RHINBOWL_88 = 3;
    public static int BELL_IDX_JAP_RHINBOWL_90 = 4;
    public static int BELL_IDX_JAP_RHINBOWL_97 = 7;
    public static int BELL_IDX_LOW_TONE = 1;
    public static int BELL_IDX_TIB_RHINBOWL_230 = 6;
    private static final String TAG = "ZMT_BellCollection";
    private static BellCollection instance;
    private ArrayList<Bell> bells = new ArrayList<>();
    private String demoBellName;

    private BellCollection() {
    }

    public static BellCollection getInstance() {
        if (instance == null) {
            instance = new BellCollection();
        }
        return instance;
    }

    public void init(Context context) {
        this.demoBellName = context.getResources().getString(R.string.bell_name_2);
        this.bells.clear();
        this.bells.add(new Bell(getPredefinedBellUri(context, R.raw.bell1), context.getResources().getString(R.string.bell_name_1)));
        this.bells.add(new Bell(getPredefinedBellUri(context, R.raw.bell2), context.getResources().getString(R.string.bell_name_2)));
        this.bells.add(new Bell(getPredefinedBellUri(context, R.raw.dharma107), context.getResources().getString(R.string.bell_name_3)));
        this.bells.add(new Bell(getPredefinedBellUri(context, R.raw.dharmaschwarz88), context.getResources().getString(R.string.bell_name_4)));
        this.bells.add(new Bell(getPredefinedBellUri(context, R.raw.shomyo90), context.getResources().getString(R.string.bell_name_5)));
        this.bells.add(new Bell(getPredefinedBellUri(context, R.raw.tang164), context.getResources().getString(R.string.bell_name_6)));
        this.bells.add(new Bell(getPredefinedBellUri(context, R.raw.tib230), context.getResources().getString(R.string.bell_name_7)));
        this.bells.add(new Bell(getPredefinedBellUri(context, R.raw.zen97), context.getResources().getString(R.string.bell_name_8)));
        File[] listFiles = context.getFilesDir().listFiles(new FilenameFilter() { // from class: de.gaffga.android.zazentimer.audio.BellCollection.1
            @Override // java.io.FilenameFilter
            public boolean accept(File file, String str) {
                return str.startsWith("bell_");
            }
        });
        for (int i = 0; i < listFiles.length; i++) {
            this.bells.add(new Bell(getCustomBellUri(context, listFiles[i].getName()), listFiles[i].getName()));
        }
    }

    private Uri getPredefinedBellUri(Context context, int i) {
        return Uri.parse("android.resource://" + context.getPackageName() + "/" + i);
    }

    private Uri getCustomBellUri(Context context, String str) {
        return Uri.parse("file://" + context.getFilesDir() + "/" + str);
    }

    @Nullable
    public Bell getBell(String str) {
        for (int i = 0; i < this.bells.size(); i++) {
            Bell bell = this.bells.get(i);
            if (bell.getName().equals(str)) {
                return bell;
            }
        }
        return null;
    }

    public ArrayList<Bell> getBellList() {
        return this.bells;
    }

    public Bell getDemoBell() {
        return getBell(this.demoBellName);
    }

    public void release() {
        instance = null;
    }

    public Bell getBell(int i) {
        if (this.bells.size() <= i || i < 0) {
            return null;
        }
        return this.bells.get(i);
    }

    public Bell getBellForSection(Section section) {
        String str = section.bellUri;
        for (int i = 0; i < this.bells.size(); i++) {
            Bell bell = this.bells.get(i);
            if (bell.getUri().toString().equals(str)) {
                return bell;
            }
        }
        return null;
    }

    public Uri getUriForName(String str) {
        for (int i = 0; i < this.bells.size(); i++) {
            if (this.bells.get(i).getUri().toString().endsWith(str)) {
                return this.bells.get(i).getUri();
            }
        }
        return null;
    }
}
