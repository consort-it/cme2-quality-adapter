package com.consort;

public class ConnectionFailedException extends RuntimeException {

    private static final long serialVersionUID = -7094347529366215444L;

    public ConnectionFailedException(String message) {
        super(message);
    }
}
