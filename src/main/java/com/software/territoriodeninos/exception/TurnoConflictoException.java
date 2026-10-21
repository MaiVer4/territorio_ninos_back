package com.software.territoriodeninos.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class TurnoConflictoException extends ConflictoException {

    public TurnoConflictoException(String message) {
        super(message);
    }
}
