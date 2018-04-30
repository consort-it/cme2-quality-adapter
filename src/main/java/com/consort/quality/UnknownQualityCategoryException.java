package com.consort.quality;

public class UnknownQualityCategoryException extends RuntimeException {

    private static final long serialVersionUID = 2055269926171776949L;

    public UnknownQualityCategoryException(String message) {
        super(message);
    }
}
