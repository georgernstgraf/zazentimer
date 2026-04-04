package de.gaffga.android.base.exceptions;

public class BundleSaveException extends Exception {
    private static final long serialVersionUID = -8409097301637768344L;

    public BundleSaveException(String str, Throwable th) {
        super(str, th);
    }

    public BundleSaveException(Throwable th) {
        super(th);
    }
}
