package de.gaffga.android.mapping.exceptions;

import android.database.SQLException;

public class MappingSQLException extends MappingException {
    private static final long serialVersionUID = 5593549748321720521L;

    public MappingSQLException(String str, SQLException sQLException) {
        super(str, sQLException);
    }

    public MappingSQLException(String str) {
        super(str);
    }
}
