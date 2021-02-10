package com.github.tornaia.jimglabel.common.json;

public class SerializerException extends RuntimeException {

    public SerializerException(String message) {
        super(message);
    }

    public SerializerException(String message, Exception cause) {
        super(message, cause);
    }
}
