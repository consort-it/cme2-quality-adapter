package com.consort.quality;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class Quality {

    private String category;

    private String generatedAt;

    private String ref;

    private String status;

    private Integer issueCount;

    private List<QualityDetails> details;
}
