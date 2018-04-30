package com.consort;

import spark.Request;

public class RequestStub extends Request {

    public String params(String param) {
        return "aws-costs-adapter";
    }
}
