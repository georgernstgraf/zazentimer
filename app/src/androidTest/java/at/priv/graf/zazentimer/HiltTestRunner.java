package at.priv.graf.zazentimer;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import androidx.test.runner.AndroidJUnitRunner;
import dagger.hilt.android.testing.HiltTestApplication;

public class HiltTestRunner extends AndroidJUnitRunner {
    @Override
    public Application newApplication(ClassLoader cl, String className, Context context)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return super.newApplication(cl, HiltTestApplication.class.getName(), context);
    }

    @Override
    public void onCreate(Bundle arguments) {
        if ("true".equals(arguments.getString("headless"))) {
            arguments.putString("notAnnotation", RequiresDisplay.class.getName());
        }
        super.onCreate(arguments);
    }
}
