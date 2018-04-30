package com.consort.jenkins;


import com.consort.quality.QualityDetails;
import lombok.AllArgsConstructor;

import static com.consort.quality.QualityType.FAILED;
import static com.consort.quality.QualityType.PASSED;
import static com.consort.quality.QualityType.UNKNOWN;
import static com.consort.quality.QualityType.WARNING;

@AllArgsConstructor
public class JenkinsService {

    private static final String PIPELINE_PREFIX = "pipeline_";

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

        if (result == null || result.getResult().equals("UNKNOWN")) {
            return getFailedRequest();
        }

        if (result.getResult().equals("SUCCESS")) {
            return getPassedInformation(result);
        }

        if (result.getResult().equals("UNSTABLE")) {
            return getWarningInformation(result);
        }

        return getFailedInformation(result);
    }

    private QualityDetails getPassedInformation(JenkinsResult result) {
        return QualityDetails.builder()
                .result(PASSED.getName())
                .ref(result.getUrl())
                .build();
    }

    private QualityDetails getWarningInformation(JenkinsResult result) {
        return QualityDetails.builder()
                .result(WARNING.getName())
                .errorMessage(String.format("Build Number %d resulted in an unstable build.", result.getNumber()))
                .ref(result.getUrl())
                .build();
    }

    private QualityDetails getFailedInformation(JenkinsResult result) {
        return QualityDetails.builder()
                .result(FAILED.getName())
                .errorMessage(String.format("Build Number %d failed.", result.getNumber()))
                .ref(result.getUrl())
                .build();
    }

    private QualityDetails getFailedRequest() {
        return QualityDetails.builder()
                .result(UNKNOWN.getName())
                .errorMessage("Failed to connect to Jenkins.")
                .build();
    }
}
