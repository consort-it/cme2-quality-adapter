package com.consort.sonar;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Builder
@AllArgsConstructor
public class SonarQubeResult {

    private List<Analysis> analyses;
}
