package com.software.territoriodeninos.exception;

public class AforoCompletoException extends RuntimeException {
    public AforoCompletoException(String mensaje) {
        super(mensaje);
    }

    public AforoCompletoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
