package de.gaffga.android.mapping.exceptions;

public class MappingException extends RuntimeException {
    private static final long serialVersionUID = 3806092406150706467L;

    public MappingException() {
    }

    public MappingException(String str) {
        super(str);
    }

    public MappingException(String str, Throwable th) {
        super(str, th);
    }
}
