package de.gaffga.android.base.exceptions;

public class BundleLoadException extends Exception {
    private static final long serialVersionUID = -2535294237675198297L;

    public BundleLoadException(String str, Throwable th) {
        super(str, th);
    }

    public BundleLoadException(Throwable th) {
        super(th);
    }
}
