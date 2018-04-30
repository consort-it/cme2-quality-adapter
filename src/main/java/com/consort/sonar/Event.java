package com.consort.sonar;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class Event {

    private String key;

    private String category;

    private String name;

    private String description;
}
