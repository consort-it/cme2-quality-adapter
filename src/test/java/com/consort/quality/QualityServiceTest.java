package com.consort.quality;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

import com.consort.Errors;
import com.consort.QualityAdapterException;
import com.consort.sonar.SonarQubeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;

import spark.Request;
import spark.Response;

public class QualityServiceTest {

    @Test
    public void should_calculate_100_quality_index() {

        // GIVEN
        List<Integer> values = Arrays.asList(0, 0);

        // WHEN / THEN
        assertThat(QualityService.generateQualityIndex(values, 5d)).isEqualTo(100);
    }

    @Test
    public void should_calculate_0_quality_index() {

        // GIVEN
        List<Integer> values = Arrays.asList(6, 7);

        // WHEN / THEN
        assertThat(QualityService.generateQualityIndex(values, 5d)).isEqualTo(0);
    }

    @Test
    public void getCumulatedCodeQualityDetails__should_throw_Exception_if_sonarQubeService_throwsException()
            throws Exception {

        // GIVEN
        Request request = mock(Request.class);
        Response response = mock(Response.class);
        SonarQubeService sonarQubeService = mock(SonarQubeService.class);
        when(request.params(":microservices")).thenReturn("test-service");
        when(sonarQubeService.getStaticCodeQualityDetails(anyString()))
                .thenThrow(new QualityAdapterException(Errors.ERR_UNKNOWN_ERROR, "test"));

        QualityService sut = new QualityService(Executors.newSingleThreadExecutor(), sonarQubeService);

        // WHEN
        assertThatExceptionOfType(QualityAdapterException.class)
                .isThrownBy(() -> sut.getCumulatedCodeQualityDetails(request, response));

    }

    @Test
    public void getCumulatedCodeQualityDetails__should_cumulate_all_measures() throws Exception {

        // GIVEN
        Request request = mock(Request.class);
        Response response = mock(Response.class);
        SonarQubeService sonarQubeService = mock(SonarQubeService.class);
        when(request.params(":microservices")).thenReturn("test-service1,test-service2");
        when(sonarQubeService.getStaticCodeQualityDetails("test-service1")).thenReturn(
                StaticCodeQualityDetails.builder().bugs(1).codeSmells(2).coverage(75.0).vulnerabilities(3).build());
        when(sonarQubeService.getStaticCodeQualityDetails("test-service2")).thenReturn(
                StaticCodeQualityDetails.builder().bugs(1).codeSmells(1).coverage(25.0).vulnerabilities(1).build());

        QualityService sut = new QualityService(Executors.newSingleThreadExecutor(), sonarQubeService);

        // WHEN
        JsonNode actual = new ObjectMapper().readTree(sut.getCumulatedCodeQualityDetails(request, response));
        assertThat(actual.get("bugs").asInt()).isEqualTo(2);
        assertThat(actual.get("codeSmells").asInt()).isEqualTo(3);
        assertThat(actual.get("coverage").asDouble()).isEqualTo(50.0);
        assertThat(actual.get("vulnerabilities").asInt()).isEqualTo(4);

    }

}
