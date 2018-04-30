package com.consort.quality;

import com.consort.jenkins.JenkinsService;
import com.consort.sonar.SonarQubeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import spark.Request;
import spark.Response;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@Slf4j
public class QualityService {

    public static String getQualityInformation(Request request, Response response) throws JsonProcessingException {

        String qualityCategory = request.params(":category").trim();
        List<String> microserviceNames = Arrays.stream(request.params(":microservices").split(",")).map(s -> s.trim()).collect(toList());
        LocalDateTime generationDateTime = LocalDateTime.now(ZoneId.of("UTC"));

        List<QualityDetails> qualityDetails = getQualityDetails(qualityCategory, microserviceNames);

        response.type("application/json");
        final ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(
                Quality.builder()
                        .category(qualityCategory)
                        .generatedAt(generationDateTime.toString())
                        .details(qualityDetails)
                        .build()
        );
    }

    private static List<QualityDetails> getSonarQubeDetails(List<String> microserviceNames) {

        SonarQubeService sonarQubeService = new SonarQubeService();
        return microserviceNames
                .stream()
                .map(service -> sonarQubeService.getSonarQubeQualityInformation(service))
                .collect(toList());
    }

    private static List<QualityDetails> getJenkinsDetails(List<String> microserviceNames) {

        JenkinsService jenkinsService = new JenkinsService();
        return microserviceNames
                .stream()
                .map(service -> jenkinsService.getJenkinsQualityInformation(service))
                .collect(toList());
    }

    private static List<QualityDetails> getQualityDetails(String category, List<String> microservices) {

        Map<String, List<QualityDetails>> qualityMap = new HashMap<>();
        qualityMap.put(QualityCategory.BUILDS.getName(), getJenkinsDetails(microservices));
        qualityMap.put(QualityCategory.CODE_QUALITY.getName(), getSonarQubeDetails(microservices));

        try {
            if (qualityMap.containsKey(category)) {
                return qualityMap.get(category);
            }

            return new ArrayList<>();
        } catch (IllegalArgumentException e) {
            log.warn("No quality categiory name {} found.", category);
            throw new UnknownQualityCategoryException(String.format("No quality categiory name '%s' found.", category));
        }
    }
}
