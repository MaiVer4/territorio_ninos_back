package com.software.territoriodeninos.exception;

public class EstacionNoAutorizadaException extends RuntimeException {
    public EstacionNoAutorizadaException(String mensaje) {
        super(mensaje);
    }

    public EstacionNoAutorizadaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
