package com.consort.sonar;

import static com.consort.quality.QualityType.PASSED;
import static com.consort.quality.QualityType.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.consort.QualityAdapterException;
import com.consort.quality.QualityDetails;
import com.consort.quality.StaticCodeQualityDetails;
import com.consort.sonar.SonarQubeClient.StaticMetric;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SonarQubeServiceTest {

    private SonarQubeClient sonarQubeClient;

    private SonarQubeService sonarQubeService;

    @Before
    public void setUp() {
        sonarQubeClient = mock(SonarQubeClient.class);
        sonarQubeService = new SonarQubeService(sonarQubeClient);
    }

    @Test
    public void getSonarQubeQualityInformation__should_get_sonar_qube_quality_check() {

        // GIVEN / WHEN
        when(sonarQubeClient.getResultByProject(anyString())).thenReturn(getSonarQubeResult());
        QualityDetails quality = sonarQubeService.getSonarQubeQualityInformation("my-service");

        // THEN
        assertThat(quality.getResult()).isEqualTo(PASSED.getName());
        assertThat(quality.getErrorMessage()).isNull();
        assertThat(quality.getRef()).isNotNull();
    }

    @Test
    public void getStaticCodeQualityDetails__should_transform_client_measures_to_StaticCodeQualityDetails() {

        // GIVEN / WHEN
        when(sonarQubeClient.requestMeasuresForMicroservice(anyString())).thenReturn(
                Arrays.stream(StaticMetric.values()).map((value) -> new Measure(value.getName(), "" + value.ordinal()))
                        .collect(Collectors.toList()));

        // THEN
        StaticCodeQualityDetails actual = sonarQubeService.getStaticCodeQualityDetails("my-service");

        assertThat(actual.getBugs()).isEqualTo(StaticMetric.BUGS.ordinal());
        assertThat(actual.getCodeSmells()).isEqualTo(StaticMetric.CODE_SMELLS.ordinal());
        assertThat(actual.getCoverage()).isEqualTo(StaticMetric.COVERAGE.ordinal());
        assertThat(actual.getVulnerabilities()).isEqualTo(StaticMetric.VULNERABILITIES.ordinal());
        assertThat(ZonedDateTime.parse(actual.getGeneratedAt()))
                .isEqualToIgnoringHours(ZonedDateTime.now(ZoneId.of("UTC")));
    }

    @Test
    public void getStaticCodeQualityDetails__should_strip_version_suffix_from_microservice_name() {

        final Pattern MICROSERVICE_NAME_PATTERN = Pattern.compile("(.+)-v.+");
        Matcher m = MICROSERVICE_NAME_PATTERN.matcher("my-service-v1");
        Matcher m2 = MICROSERVICE_NAME_PATTERN.matcher("my-service");
        Matcher m3 = MICROSERVICE_NAME_PATTERN.matcher("-v1");
        // GIVEN / WHEN
        try {
            sonarQubeService.getStaticCodeQualityDetails("my-service-v1");
        } catch (Exception e) {
        }

        // THEN
        verify(sonarQubeClient).requestMeasuresForMicroservice("my-service");
    }

    private SonarQubeResult getSonarQubeResult() {
        Event event = Event.builder().category("QUALITY_GATE").description("This is a quality gate.")
                .name("Green (was Red)").key("key").build();

        Analysis analysis = Analysis.builder().date("2018-01-01").key("key").events(Arrays.asList(event)).build();

        List<Analysis> analysisList = Arrays.asList(analysis);
        return SonarQubeResult.builder().analyses(analysisList).build();
    }

    private SonarQubeResult getEmptyResult() {

        List<Analysis> analysisList = new ArrayList<>();
        return SonarQubeResult.builder().analyses(analysisList).build();
    }

    @Test
    public void getSonarQubeQualityInformation__should_get_empty_information_result() {

        // GIVEN / WHEN
        when(sonarQubeClient.getResultByProject(anyString())).thenReturn(getEmptyResult());
        QualityDetails quality = sonarQubeService.getSonarQubeQualityInformation("my-service");

        // THEN
        assertThat(quality.getResult()).isEqualTo(UNKNOWN.getName());
        assertThat(quality.getErrorMessage()).isNotNull();
    }
}
