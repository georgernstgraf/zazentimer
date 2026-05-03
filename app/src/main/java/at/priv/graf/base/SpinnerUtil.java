package at.priv.graf.base;

import at.priv.graf.zazentimer.bo.Session;
import java.util.ArrayList;

public class SpinnerUtil {
    public static int getPositionById(ArrayList<Session> arrayList, int i) {
        for (int i2 = 0; i2 < arrayList.size(); i2++) {
            if (arrayList.get(i2).id == i) {
                return i2;
            }
        }
        return -1;
    }
}
