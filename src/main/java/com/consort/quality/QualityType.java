package com.consort.quality;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum QualityType {

    PASSED("Passed"),
    WARNING("Warning"),
    UNKNOWN("Unkown"),
    FAILED("Failed");

    private String name;
}
