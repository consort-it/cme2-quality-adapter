package com.consort.sonar;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Builder
@AllArgsConstructor
public class Measure {

    private String metric;

    private String value;
}
