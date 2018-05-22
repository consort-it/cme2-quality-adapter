package com.consort.sonar;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.consort.ConnectionFailedException;
import com.consort.Errors;
import com.consort.QualityAdapterException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SonarQubeClient {
    @Getter
    @AllArgsConstructor
    public static enum StaticMetric {
        COVERAGE("coverage"), CODE_SMELLS("code_smells"), VULNERABILITIES("vulnerabilities"), BUGS("bugs");

        private String name;

        protected static String valuesAsMetricKeysUriParameters() {
            return Arrays.stream(values()).map(value -> value.name).collect(Collectors.joining(","));
        }

        protected static Set<String> getAllNames() {
            return Arrays.stream(values()).map(v -> v.name).collect(Collectors.toSet());
        }
    }

    private static final String SONAR_QUBE_BASE_URI = "https://cme.dev.k8s.consort-it.de/sonar/api";
    private static final String PROJECT_ANALYSIS = "project_analyses/search?ps=1";
    private static final String MEASURES = "measures/component_tree?metricKeys="
            + StaticMetric.valuesAsMetricKeysUriParameters();

    private static final String SONAR_QUBE_TOKEN = "SONAR_QUBE_TOKEN";

    public SonarQubeResult getResultByProject(@NonNull final String microservice) {

        log.info("Request last sonar check for service {}", microservice);

        String sonarQubeUri = String.format("%s/%s&project=%s", SONAR_QUBE_BASE_URI, PROJECT_ANALYSIS, microservice);

        try {
            HttpURLConnection connection = getConnection(sonarQubeUri);
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);

            InputStream content = connection.getInputStream();
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(content, SonarQubeResult.class);
        } catch (IOException e) {
            log.error(e.getLocalizedMessage());
            return getFailedSonarQubeRequestResult();
        }
    }

    /**
     * Uses SonarQube WebAPI endpoint /sonar/api/measures/component_tree to request
     * measures for the given microservice.
     * 
     * @return list of Measures
     * @throws QualityAdapterException if microservice is invalid or something is
     *                                 wrong with the reponse from SonarQube
     * @throws RuntimeException        if an unexpected exception occured
     */
    public List<Measure> requestMeasuresForMicroservice(@NonNull final String microservice)
            throws QualityAdapterException, RuntimeException {
        log.info("Request measures for service {}", microservice);

        String sonarQubeUri = String.format("%s/%s&component=%s", SONAR_QUBE_BASE_URI, MEASURES, microservice);

        List<Measure> measures = new ArrayList<Measure>();

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(sonarQubeUri).openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);

            InputStream content = connection.getInputStream();
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.readTree(content).get("baseComponent").get("measures").forEach(measure -> measures
                    .add(new Measure(measure.get("metric").asText(), measure.get("value").asText())));
        } catch (FileNotFoundException e) {
            log.error("FileNotFoundException while requesting measures", e);
            throw new QualityAdapterException(Errors.ERR_NOT_FOUND, "Request measures for service " + microservice);
        } catch (Exception e) {
            log.error("Error while requesting measures", e);
            throw new QualityAdapterException(Errors.ERR_UNKNOWN_ERROR, "Request measures for service " + microservice);
        }

        if (!measures.stream().map(m -> m.getMetric()).collect(Collectors.toSet())
                .containsAll(StaticMetric.getAllNames())) {
            throw new QualityAdapterException(Errors.ERR_UNKNOWN_ERROR,
                    "Missing values while requesting measures for service " + microservice);
        }
        return measures;
    }

    private HttpURLConnection getConnection(String url) {

        String sonarQubeToken = System.getenv(SONAR_QUBE_TOKEN);

        try {
            String basicAuth = Base64.getEncoder().encodeToString((sonarQubeToken + ":").getBytes("UTF-8"));
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestProperty("Authorization", "Basic " + basicAuth);
            return connection;
        } catch (IOException e) {
            log.warn("Error connecting to SonarQube url {}", url, e);
            throw new ConnectionFailedException("Failure connecting to SonarQube.");
        }
    }

    private SonarQubeResult getFailedSonarQubeRequestResult() {

        return SonarQubeResult.builder().build();
    }

}
