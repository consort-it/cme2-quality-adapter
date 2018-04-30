package com.consort.sonar;

import com.consort.quality.QualityDetails;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.consort.quality.QualityType.PASSED;
import static com.consort.quality.QualityType.UNKNOWN;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    public void should_get_sonar_qube_quality_check() {

        // GIVEN / WHEN
        when(sonarQubeClient.getResultByProject(anyString())).thenReturn(getSonarQubeResult());
        QualityDetails quality = sonarQubeService.getSonarQubeQualityInformation("my-service");

        // THEN
        assertThat(quality.getResult()).isEqualTo(PASSED.getName());
        assertThat(quality.getErrorMessage()).isNull();
        assertThat(quality.getRef()).isNotNull();
    }

    @Test
    public void should_get_empty_information_result() {

        // GIVEN / WHEN
        when(sonarQubeClient.getResultByProject(anyString())).thenReturn(getEmptyResult());
        QualityDetails quality = sonarQubeService.getSonarQubeQualityInformation("my-service");

        // THEN
        assertThat(quality.getResult()).isEqualTo(UNKNOWN.getName());
        assertThat(quality.getErrorMessage()).isNotNull();
    }

    private SonarQubeResult getSonarQubeResult() {
        Event event = Event.builder()
                .category("QUALITY_GATE")
                .description("This is a quality gate.")
                .name("Green (was Red)")
                .key("key")
                .build();

        Analysis analysis = Analysis.builder()
                .date("2018-01-01")
                .key("key")
                .events(Arrays.asList(event))
                .build();

        List<Analysis> analysisList = Arrays.asList(analysis);
        return SonarQubeResult.builder()
                .analyses(analysisList)
                .build();
    }

    private SonarQubeResult getEmptyResult() {

        List<Analysis> analysisList = new ArrayList<>();
        return SonarQubeResult.builder()
                .analyses(analysisList)
                .build();
    }
}
