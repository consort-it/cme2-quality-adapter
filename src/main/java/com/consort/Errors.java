package com.consort;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum Errors {

    ERR_UNKNOWN_ERROR("500","Unmapped Error ocurred! Aborted."),
    ERR_AUTH_FORBIDDEN("401","Forbidden! Aborted."),
    ERR_AUTH_REQUIRED("403","Authentication required! Aborted."),
    ERR_NOT_FOUND("404","Requested URI is not available! Aborted.");

    private Integer status;
    private String message;

    Errors(String... error) {
        this.status = Integer.parseInt(Arrays.asList(error).get(0));
        this.message = Arrays.asList(error).get(1);
    }
}
