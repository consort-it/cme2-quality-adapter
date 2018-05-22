package com.consort.quality;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
public class StaticCodeQualityDetails {
    private int bugs;

    private int vulnerabilities;

    private int codeSmells;

    private double coverage;

    private String generatedAt;
}
