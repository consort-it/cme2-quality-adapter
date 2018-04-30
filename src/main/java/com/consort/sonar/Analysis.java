package com.consort.sonar;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class Analysis {

    private String key;

    private String date;

    private List<Event> events;
}
