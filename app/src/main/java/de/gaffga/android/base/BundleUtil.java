package de.gaffga.android.base;

import android.os.Bundle;
import de.gaffga.android.base.annotations.SaveToBundle;
import de.gaffga.android.base.exceptions.BundleLoadException;
import de.gaffga.android.base.exceptions.BundleSaveException;
import java.lang.reflect.Field;

public class BundleUtil {
    public static void saveToBundle(Object obj, Bundle bundle) throws BundleSaveException {
        if (obj == null) {
            return;
        }
        try {
            for (Field field : obj.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(SaveToBundle.class)) {
                    if (field.getType().getSimpleName().equals("String")) {
                        bundle.putString(field.getName(), (String) field.get(obj));
                    } else if (field.getType().getSimpleName().equals("int")) {
                        bundle.putInt(field.getName(), ((Integer) field.get(obj)).intValue());
                    }
                }
            }
        } catch (Exception e) {
            throw new BundleSaveException("Could not save to bundle", e);
        }
    }

    public static void loadFromBundle(Object obj, Bundle bundle) throws BundleLoadException {
        if (obj == null) {
            return;
        }
        try {
            for (Field field : obj.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(SaveToBundle.class)) {
                    if (field.getType().getSimpleName().equals("String")) {
                        field.set(obj, bundle.getString(field.getName()));
                    } else if (field.getType().getSimpleName().equals("int")) {
                        field.setInt(obj, bundle.getInt(field.getName()));
                    }
                }
            }
        } catch (Exception e) {
            throw new BundleLoadException("Could not load from bundle", e);
        }
    }
}
