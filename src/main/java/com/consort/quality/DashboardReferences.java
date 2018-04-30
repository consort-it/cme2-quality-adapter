package com.consort.quality;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class DashboardReferences {

    private static final String SONARQUBE_DASHBOARD_BASE_URI = "https://cme.dev.k8s.consort-it.de/sonar";

    private static final String JENKINS_DASHBOARD_BASE_URI = "http://jenkins.consort-it.de/job";

    public static String getSonarQubeProjectUrl(String microservice) {

        return String.format("%s/dashboard?id=%s", SONARQUBE_DASHBOARD_BASE_URI, microservice);
    }

    public static String getJenkinsProjectUrl(String microservice) {

        return String.format("%s/pipeline_%s", JENKINS_DASHBOARD_BASE_URI, microservice);
    }

}
