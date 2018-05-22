package com.consort.quality;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class QualityIndex {

    private String generatedAt;

    private Integer value;
}
