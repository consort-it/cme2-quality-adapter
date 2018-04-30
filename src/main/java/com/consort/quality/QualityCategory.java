package com.consort.quality;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum QualityCategory {

    CODE_QUALITY("CodeQuality"),
    BUILDS("Builds"),
    CONTRACTS("Contracts"),
    VULNERABILITIES("Vulnerabilities"),
    BUGS("Bugs"),
    SMOKE_TESTS("SmokeTests"),
    END_TO_END("End2End"),
    LOGS("Logs");

    private String name;
}
