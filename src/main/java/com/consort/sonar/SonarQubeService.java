package com.consort.sonar;

import static com.consort.quality.QualityType.FAILED;
import static com.consort.quality.QualityType.PASSED;
import static com.consort.quality.QualityType.UNKNOWN;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.consort.QualityAdapterException;
import com.consort.quality.DashboardReferences;
import com.consort.quality.QualityDetails;
import com.consort.quality.StaticCodeQualityDetails;
import com.consort.sonar.SonarQubeClient.StaticMetric;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class SonarQubeService {
    private static final String QUALITY_GATE = "QUALITY_GATE";
    private static final String QUALITY_CHECK_PASSED_PATTERN = "Green (.*)";
    private static final String QUALITY_CHECK_FAILED_PATTERN = "Red (.*)";
    private static final Pattern MICROSERVICE_NAME_PATTERN = Pattern.compile("(.+)-v.+");

    private final SonarQubeClient sonarQubeClient;

    public SonarQubeService() {
        this.sonarQubeClient = new SonarQubeClient();
    }

    public QualityDetails getSonarQubeQualityInformation(String microservice) {

        SonarQubeResult result = sonarQubeClient.getResultByProject(microservice);
        String generatedAt = LocalDateTime.now(ZoneId.of("UTC")).toString();

        if (result.getAnalyses() == null) {
            return getNoInformationResponse(microservice, generatedAt);
        }

        if (result.getAnalyses().isEmpty()) {
            return getFailedRequest(microservice, generatedAt);
        }

        generatedAt = result.getAnalyses().get(0).getDate();
        Optional<Event> event = result.getAnalyses().get(0).getEvents().stream()
                .filter(e -> e.getCategory().equals(QUALITY_GATE)).findFirst();

        if (event.isPresent()) {

            if (event.get().getName().matches(QUALITY_CHECK_PASSED_PATTERN)) {
                return getPassedSonarCheck(microservice, generatedAt);
            }

            if (event.get().getName().matches(QUALITY_CHECK_FAILED_PATTERN)) {
                return getFailedSonarCheck(microservice, event.get().getDescription(), generatedAt);
            }

            throw new RuntimeException("Unknown quality information parsed!");

        } else {
            return getPassedSonarCheck(microservice, generatedAt);
        }
    }

    /**
     * Gets details about static code quality of the given service.
     * 
     * @throws QualityAdapterException if microservice is invalid
     * @throws RuntimeException        if an unexpected exception occured
     */
    public StaticCodeQualityDetails getStaticCodeQualityDetails(String microservice)
            throws QualityAdapterException, RuntimeException {
        String sanitizedMicroserviceName = sanitizeMicroserviceName(microservice);
        List<Measure> measures = sonarQubeClient.requestMeasuresForMicroservice(sanitizedMicroserviceName);
        return buildStaticCodeQualityDetails(measures);
    }

    /**
     * Strips the "-vX" version suffix from the microservice name if existing
     */
    private static String sanitizeMicroserviceName(String name) {
        Matcher m = MICROSERVICE_NAME_PATTERN.matcher(name);
        if (m.matches() && m.groupCount() > 0) {
            return m.group(1);
        } else {
            return name;
        }
    }

    private StaticCodeQualityDetails buildStaticCodeQualityDetails(List<Measure> measures) {
        int bugs = Integer.parseInt(getMeasureValueFor(measures, StaticMetric.BUGS.getName()));
        int codeSmells = Integer.parseInt(getMeasureValueFor(measures, StaticMetric.CODE_SMELLS.getName()));
        int vulnerabilities = Integer.parseInt(getMeasureValueFor(measures, StaticMetric.VULNERABILITIES.getName()));
        double coverage = Double.parseDouble(getMeasureValueFor(measures, StaticMetric.COVERAGE.getName()));

        return StaticCodeQualityDetails.builder().bugs(bugs).codeSmells(codeSmells).vulnerabilities(vulnerabilities)
                .coverage(coverage).generatedAt(ZonedDateTime.now(ZoneId.of("UTC")).toString()).build();
    }

    private String getMeasureValueFor(List<Measure> measures, String metric) {
        return measures.stream().filter(measure -> metric.equals(measure.getMetric())).findFirst().get().getValue();
    }

    private QualityDetails getPassedSonarCheck(String service, String generatedAt) {

        return QualityDetails.builder().serviceName(service).result(PASSED.getName()).generatedAt(generatedAt)
                .ref(DashboardReferences.getSonarQubeProjectUrl(service)).build();
    }

    private QualityDetails getFailedSonarCheck(String service, String errorMessage, String generatedAt) {

        return QualityDetails.builder().serviceName(service).result(FAILED.getName()).generatedAt(generatedAt)
                .errorMessage(errorMessage).ref(DashboardReferences.getSonarQubeProjectUrl(service)).build();
    }

    private QualityDetails getFailedRequest(String service, String generatedAt) {

        return QualityDetails.builder().serviceName(service).generatedAt(generatedAt).result(UNKNOWN.getName())
                .errorMessage("Failed to connect to SonarQube.")
                .ref(DashboardReferences.getSonarQubeProjectUrl(service)).build();
    }

    private QualityDetails getNoInformationResponse(String service, String generatedAt) {

        return QualityDetails.builder().serviceName(service).generatedAt(generatedAt).result(UNKNOWN.getName())
                .errorMessage("Could not retrieve any information from SonarQube.")
                .ref(DashboardReferences.getSonarQubeProjectUrl(service)).build();
    }
}
