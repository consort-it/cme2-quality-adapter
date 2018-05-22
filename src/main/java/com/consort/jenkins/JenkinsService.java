package com.consort.jenkins;


import com.consort.quality.QualityDetails;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static com.consort.quality.QualityType.FAILED;
import static com.consort.quality.QualityType.PASSED;
import static com.consort.quality.QualityType.UNKNOWN;
import static com.consort.quality.QualityType.WARNING;

@AllArgsConstructor
public class JenkinsService {

    private static final String PIPELINE_PREFIX = "";

    private final JenkinsClient client;

    public JenkinsService() {
        client = new JenkinsClient();
    }

    public QualityDetails getJenkinsQualityInformation(String microservice) {

        String jenkinsJobName = PIPELINE_PREFIX + microservice;
        if (microservice.equals("cme-ui")) {
            // TODO make cme-ui accessible
            return QualityDetails.builder()
                    .result(WARNING.getName())
                    .errorMessage("Information can not be accessed yet.")
                    .ref("http://jenkins.consort-it.de/job/multibranch_pipelinejob_cme-ui/")
                    .build();
        }

        JenkinsResult result = client.getResultByProject(jenkinsJobName);
        String generatedAt = LocalDateTime.now(ZoneId.of("UTC")).toString();

        if (result == null || result.getResult().equals("UNKNOWN")) {
            return getFailedRequest(microservice, generatedAt);
        }

        if (result.getResult().equals("SUCCESS")) {
            return getPassedInformation(result, microservice, generatedAt);
        }

        if (result.getResult().equals("UNSTABLE")) {
            return getWarningInformation(result, microservice, generatedAt);
        }

        return getFailedInformation(result, microservice, generatedAt);
    }

    private QualityDetails getPassedInformation(JenkinsResult result, String serviceName, String generatedAt) {
        return QualityDetails.builder()
                .serviceName(serviceName)
                .result(PASSED.getName())
                .generatedAt(generatedAt)
                .ref(result.getUrl())
                .build();
    }

    private QualityDetails getWarningInformation(JenkinsResult result, String serviceName, String generatedAt) {
        return QualityDetails.builder()
                .serviceName(serviceName)
                .result(WARNING.getName())
                .generatedAt(generatedAt)
                .errorMessage(String.format("Build Number %d resulted in an unstable build.", result.getNumber()))
                .ref(result.getUrl())
                .build();
    }

    private QualityDetails getFailedInformation(JenkinsResult result, String serviceName, String generatedAt) {
        return QualityDetails.builder()
                .serviceName(serviceName)
                .result(FAILED.getName())
                .generatedAt(generatedAt)
                .errorMessage(String.format("Build Number %d failed.", result.getNumber()))
                .ref(result.getUrl())
                .build();
    }

    private QualityDetails getFailedRequest(String serviceName, String generatedAt) {
        return QualityDetails.builder()
                .serviceName(serviceName)
                .result(UNKNOWN.getName())
                .generatedAt(generatedAt)
                .errorMessage("Failed to connect to Jenkins.")
                .build();
    }
}
