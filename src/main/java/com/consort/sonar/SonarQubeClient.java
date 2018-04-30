package com.consort.sonar;

import com.consort.ConnectionFailedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

@Slf4j
public class SonarQubeClient {

    private static final String SONAR_QUBE_BASE_URI = "https://cme.dev.k8s.consort-it.de/sonar/api";
    private static final String PROJECT_ANALYSIS = "project_analyses/search?ps=1";

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
