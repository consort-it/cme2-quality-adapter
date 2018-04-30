package com.consort.jenkins;

import com.consort.ConnectionFailedException;
import com.consort.cognito.Token;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

@Slf4j
public class JenkinsClient {

    private static final String JENKINS_API_BASE_URI = "https://cme.dev.k8s.consort-it.de/api/v1/jenkins-adapter";
    private static final String AUTHORIZATION_URL = "AUTHORIZATION_URL";
    private static final String COGNITO_USER = "COGNITO_USER";
    private static final String COGNITO_SECRET = "COGNITO_SECRET";

    public JenkinsResult getResultByProject(@NonNull final String serviceName) {

        String jenkinsApiUri = String.format("%s/%s/lastbuild", JENKINS_API_BASE_URI, serviceName);

        try {
            HttpURLConnection connection = getConnection(jenkinsApiUri);
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);

            InputStream content = connection.getInputStream();
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(content, JenkinsResult.class);
        } catch (IOException e) {
            log.error(e.getLocalizedMessage());
            return getFailedJenkinsRequestResult();
        }
    }

    private HttpURLConnection getConnection(String url) {

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + getToken().getAccessToken());
            return connection;
        } catch (IOException e) {
            log.warn("Error connecting to Jenkins url {}", url, e);
            throw new ConnectionFailedException("Failure connecting to Jenkins.");
        }
    }

    private Token getToken() {

        String authorizationUrl = System.getenv(AUTHORIZATION_URL);

        try {
            HttpURLConnection connection = getTokenConnection(authorizationUrl);
            InputStream content = connection.getInputStream();

            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(content, Token.class);
        } catch (IOException e) {
            log.warn("Error connecting to Cognito url {}", authorizationUrl, e);
            return Token.builder()
                    .accessToken("accessToken")
                    .build();
        }
    }

    private HttpURLConnection getTokenConnection(String authorizationUrl) throws IOException {

        String cognitoUserName = System.getenv(COGNITO_USER);
        String cognitoPassword = System.getenv(COGNITO_SECRET);

        String basicAuth = Base64.getEncoder().encodeToString((cognitoUserName + ":" + cognitoPassword).getBytes("UTF-8"));
        HttpURLConnection connection = (HttpURLConnection) new URL(authorizationUrl).openConnection();

        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("Authorization", "Basic " + basicAuth);

        connection.setDoOutput(true);
        connection.setDoInput(true);

        String params = "grant_type=client_credentials&scope=jenkins-adapter/read";
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(connection.getOutputStream());
        outputStreamWriter.write(params);
        outputStreamWriter.flush();

        return connection;
    }

    private JenkinsResult getFailedJenkinsRequestResult() {

        return JenkinsResult.builder()
                .result("UNKOWN")
                .build();
    }

}
