package com.consort.jenkins;

import com.consort.quality.QualityDetails;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static com.consort.quality.QualityType.FAILED;
import static com.consort.quality.QualityType.PASSED;
import static com.consort.quality.QualityType.UNKNOWN;
import static com.consort.quality.QualityType.WARNING;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JenkinsServiceTest {

    private JenkinsClient jenkinsClient;

    private JenkinsService jenkinsService;

    @Before
    public void setUp() {

        jenkinsClient = mock(JenkinsClient.class);
        jenkinsService = new JenkinsService(jenkinsClient);
    }

    @Test
    public void should_get_jenkins_last_build_as_passed() {

        // GIVEN / WHEN
        when(jenkinsClient.getResultByProject(anyString())).thenReturn(getJenkinsSuccessResult());
        QualityDetails quality = jenkinsService.getJenkinsQualityInformation("my-service");

        // THEN
        assertThat(quality.getResult()).isEqualTo(PASSED.getName());
    }

    @Test
    public void should_get_jenkins_last_build_as_warning() {

        // GIVEN / WHEN
        when(jenkinsClient.getResultByProject(anyString())).thenReturn(getJenkinsUnstableResult());
        QualityDetails quality = jenkinsService.getJenkinsQualityInformation("my-service");

        // THEN
        assertThat(quality.getResult()).isEqualTo(WARNING.getName());
    }

    @Test
    public void should_get_jenkins_last_build_as_failed() {

        // GIVEN / WHEN
        when(jenkinsClient.getResultByProject(anyString())).thenReturn(getJenkinsFailedResult());
        QualityDetails quality = jenkinsService.getJenkinsQualityInformation("my-service");

        // THEN
        assertThat(quality.getResult()).isEqualTo(FAILED.getName());
    }

    @Test
    public void should_get_failed_connection_information() {

        // GIVEN / WHEN
        when(jenkinsClient.getResultByProject(anyString())).thenReturn(null);
        QualityDetails quality = jenkinsService.getJenkinsQualityInformation("my-service");

        // THEN
        assertThat(quality.getResult()).isEqualTo(UNKNOWN.getName());
    }

    private JenkinsResult getJenkinsSuccessResult() {
        return JenkinsResult.builder()
                .number(42)
                .result("SUCCESS")
                .url("https://jenkins.consort-it.de/job/my-service")
                .build();
    }

    private JenkinsResult getJenkinsUnstableResult() {
        return JenkinsResult.builder()
                .number(42)
                .result("UNSTABLE")
                .url("https://jenkins.consort-it.de/job/my-service")
                .build();
    }

    private JenkinsResult getJenkinsFailedResult() {
        return JenkinsResult.builder()
                .number(42)
                .result("FAILED")
                .url("https://jenkins.consort-it.de/job/my-service")
                .build();
    }
}
