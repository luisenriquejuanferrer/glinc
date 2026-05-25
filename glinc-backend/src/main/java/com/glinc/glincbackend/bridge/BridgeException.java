package com.glinc.glincbackend.bridge;

public class BridgeException extends RuntimeException {

    public BridgeException(String mensaje) {
        super(mensaje);
    }

    public BridgeException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
