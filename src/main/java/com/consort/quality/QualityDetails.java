package com.consort.quality;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class QualityDetails {

    private String serviceName;

    private String result;

    private String errorMessage;

    private String ref;
}
