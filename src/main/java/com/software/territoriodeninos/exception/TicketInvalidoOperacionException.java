package com.software.territoriodeninos.exception;

import org.springframework.http.HttpStatus;

public class TicketInvalidoOperacionException extends RuntimeException {
    private final HttpStatus status;

    public TicketInvalidoOperacionException(String mensaje) {
        super(mensaje);
        this.status = HttpStatus.CONFLICT;
    }

    public TicketInvalidoOperacionException(String mensaje, HttpStatus status) {
        super(mensaje);
        this.status = status != null ? status : HttpStatus.CONFLICT;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
