package com.consort.sonar;

import com.consort.quality.DashboardReferences;
import com.consort.quality.QualityDetails;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.Optional;

import static com.consort.quality.QualityType.FAILED;
import static com.consort.quality.QualityType.PASSED;
import static com.consort.quality.QualityType.UNKNOWN;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class SonarQubeService {

    private static final String QUALITY_GATE = "QUALITY_GATE";

    private static final String QUALITY_CHECK_PASSED_PATTERN = "Green (.*)";

    private static final String QUALITY_CHECK_FAILED_PATTERN = "Red (.*)";

    private final SonarQubeClient sonarQubeClient;

    public SonarQubeService() {
        this.sonarQubeClient = new SonarQubeClient();
    }

    public QualityDetails getSonarQubeQualityInformation(String microservice) {

        SonarQubeResult result = sonarQubeClient.getResultByProject(microservice);

        if (result.getAnalyses() == null) {
            return getNoInformationResponse(microservice);
        }

        if (result.getAnalyses().isEmpty()) {
            return getFailedRequest(microservice);
        }

        Optional<Event> event = result.getAnalyses().get(0).getEvents().stream()
                .filter(e -> e.getCategory().equals(QUALITY_GATE))
                .findFirst();

        if (event.isPresent()) {

            if (event.get().getName().matches(QUALITY_CHECK_PASSED_PATTERN)) {
                return getPassedSonarCheck(microservice);
            }

            if (event.get().getName().matches(QUALITY_CHECK_FAILED_PATTERN)) {
                return getFailedSonarCheck(microservice, event.get().getDescription());
            }

            throw new RuntimeException("Unknown quality information parsed!");

        } else {
            return getPassedSonarCheck(microservice);
        }
    }

    private QualityDetails getPassedSonarCheck(String service) {

        return QualityDetails.builder()
                .serviceName(service)
                .result(PASSED.getName())
                .ref(DashboardReferences.getSonarQubeProjectUrl(service))
                .build();
    }

    private QualityDetails getFailedSonarCheck(String service, String errorMessage) {

        return QualityDetails.builder()
                .serviceName(service)
                .result(FAILED.getName())
                .errorMessage(errorMessage)
                .ref(DashboardReferences.getSonarQubeProjectUrl(service))
                .build();
    }

    private QualityDetails getFailedRequest(String service) {

        return QualityDetails.builder()
                .serviceName(service)
                .result(UNKNOWN.getName())
                .errorMessage("Failed to connect to SonarQube.")
                .ref(DashboardReferences.getSonarQubeProjectUrl(service))
                .build();
    }

    private QualityDetails getNoInformationResponse(String service) {

        return QualityDetails.builder()
                .serviceName(service)
                .result(UNKNOWN.getName())
                .errorMessage("Could not retrieve any information from SonarQube.")
                .ref(DashboardReferences.getSonarQubeProjectUrl(service))
                .build();
    }
}
