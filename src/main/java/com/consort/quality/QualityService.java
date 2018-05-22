package com.consort.quality;

import static com.consort.quality.QualityType.FAILED;
import static com.consort.quality.QualityType.PASSED;
import static com.consort.quality.QualityType.UNKNOWN;
import static com.consort.quality.QualityType.WARNING;
import static java.util.stream.Collectors.toList;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import com.consort.jenkins.JenkinsService;
import com.consort.sonar.SonarQubeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import spark.Request;
import spark.Response;

@Slf4j
@AllArgsConstructor
public class QualityService {
    private final ExecutorService threadPool;
    private final SonarQubeService sonarQubeService;

    public QualityService() {
        threadPool = Executors.newFixedThreadPool(5);
        sonarQubeService = new SonarQubeService();
    }

    public static String getQualityInformation(Request request, Response response) throws JsonProcessingException {

        String qualityCategory = request.params(":category").trim();
        List<String> microserviceNames = parseRequestedMicroserviceNames(request);
        LocalDateTime generationDateTime = LocalDateTime.now(ZoneId.of("UTC"));

        List<QualityDetails> qualityDetails = getQualityDetails(qualityCategory, microserviceNames);
        List<String> statusList = qualityDetails.stream().map(details -> details.getResult()).collect(toList());

        String aggregatedStatus = PASSED.getName();
        Integer issues = 0;

        if (statusList.contains(WARNING.getName())) {
            aggregatedStatus = WARNING.getName();
            issues = Math.toIntExact(statusList.stream().filter(element -> element.equals(WARNING.getName())).count());
        }

        if (statusList.contains(FAILED.getName())) {
            aggregatedStatus = FAILED.getName();
            issues = Math.toIntExact(statusList.stream().filter(element -> element.equals(FAILED.getName())).count());
        }

        if (statusList.contains(UNKNOWN.getName())) {
            Integer unknownCount = Math
                    .toIntExact(statusList.stream().filter(element -> element.equals(FAILED.getName())).count());
            if (unknownCount == statusList.size()) {
                aggregatedStatus = UNKNOWN.getName();
            }
        }

        response.type("application/json");
        final ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(Quality.builder().category(qualityCategory)
                .ref(getDashboardUrl(qualityCategory)).generatedAt(generationDateTime.toString())
                .status(aggregatedStatus).issueCount(issues).details(qualityDetails).build());
    }

    public static String getQualityIndex(Request request, Response response) throws JsonProcessingException {

        LocalDateTime generationDateTime = LocalDateTime.now(ZoneId.of("UTC"));
        List<String> microserviceNames = parseRequestedMicroserviceNames(request);

        Map<String, Integer> issueCounts = new HashMap<>();
        Arrays.stream(QualityCategory.values()).forEach(category -> {

            if (category.getName().equals("CodeQuality") || category.getName().equals("Builds")) {
                List<QualityDetails> qualityDetails = getQualityDetails(category.getName(), microserviceNames);
                List<String> statusList = qualityDetails.stream().map(details -> details.getResult()).collect(toList());
                statusList.stream().filter(status -> status.equals(FAILED.getName())).count();
                issueCounts.put(category.getName(),
                        (int) statusList.stream().filter(status -> status.equals(FAILED.getName())).count());
            }
        });

        List<Integer> issueList = new ArrayList<>(issueCounts.values());
        Integer qualityIndex = generateQualityIndex(issueList, 5d);

        return prepareJsonResponse(
                QualityIndex.builder().generatedAt(generationDateTime.toString()).value(qualityIndex).build(),
                response);
    }

    private static String prepareJsonResponse(Object body, Response response) throws JsonProcessingException {
        response.type("application/json");
        final ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(body);
    }

    private static List<String> parseRequestedMicroserviceNames(Request request) {
        List<String> microserviceNames = Arrays.stream(request.params(":microservices").split(",")).map(s -> s.trim())
                .collect(toList());
        return microserviceNames;
    }

    public String getCumulatedCodeQualityDetails(Request request, Response response)
            throws InterruptedException, JsonProcessingException, RuntimeException {
        List<String> targetMicroservices = parseRequestedMicroserviceNames(request);

        List<Future<StaticCodeQualityDetails>> resultFuturesPerMicroservice = threadPool
                .invokeAll(targetMicroservices.stream().map(microservice -> new Callable<StaticCodeQualityDetails>() {
                    public StaticCodeQualityDetails call() {
                        return sonarQubeService.getStaticCodeQualityDetails(microservice);
                    }
                }).collect(Collectors.toList()));

        List<StaticCodeQualityDetails> results = new ArrayList<>();
        for (Future<StaticCodeQualityDetails> future : resultFuturesPerMicroservice) {
            try {
                results.add(future.get());
            } catch (ExecutionException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new RuntimeException(e.getCause());
                }
            }
        }

        StaticCodeQualityDetails cumulatedResult = results.stream().reduce(StaticCodeQualityDetails.builder().build(),
                (a, b) -> new StaticCodeQualityDetails(a.getBugs() + b.getBugs(),
                        a.getVulnerabilities() + b.getVulnerabilities(), a.getCodeSmells() + b.getCodeSmells(),
                        a.getCoverage() + b.getCoverage(), b.getGeneratedAt()));

        cumulatedResult.setCoverage(cumulatedResult.getCoverage() / (double) targetMicroservices.size());

        return prepareJsonResponse(cumulatedResult, response);
    }

    private static List<QualityDetails> getSonarQubeDetails(List<String> microserviceNames) {
        SonarQubeService sonarQubeService = new SonarQubeService();
        return microserviceNames.stream().map(sonarQubeService::getSonarQubeQualityInformation).collect(toList());
    }

    private static List<QualityDetails> getJenkinsDetails(List<String> microserviceNames) {

        JenkinsService jenkinsService = new JenkinsService();
        return microserviceNames.stream().map(jenkinsService::getJenkinsQualityInformation).collect(toList());
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
            log.warn("No quality category name {} found.", category);
            throw new UnknownQualityCategoryException(String.format("No quality category name '%s' found.", category));
        }
    }

    private static String getDashboardUrl(String category) {

        Map<String, String> urlMap = new HashMap<>();
        urlMap.put(QualityCategory.BUILDS.getName(), "http://jenkins.consort-it.de/");
        urlMap.put(QualityCategory.CODE_QUALITY.getName(),
                "https://cme.dev.k8s.consort-it.de/sonar/projects?sort=-analysis_date");

        try {
            if (urlMap.containsKey(category)) {
                return urlMap.get(category);
            }

            return "No reference found!";
        } catch (IllegalArgumentException e) {
            log.warn("No quality category name {} found.", category);
            throw new UnknownQualityCategoryException(String.format("No quality category name '%s' found.", category));
        }
    }

    static Integer generateQualityIndex(List<Integer> issueList, Double cappedFailureCount) {

        Integer numberOfCategories = issueList.size();
        Double qualityWeight = cappedFailureCount * numberOfCategories;

        List<Double> qualities = issueList.stream().map(value -> Math.min(value, cappedFailureCount) / qualityWeight)
                .collect(toList());
        Double sum = qualities.stream().mapToDouble(Double::doubleValue).sum();

        return (int) Math.round((1 - sum) * 100);
    }
}
