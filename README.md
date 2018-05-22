# Quality Adapter

The Quality Adapter aggregates information from tools, such as Jenkins and SonarQube, and summarizes them. The information is displayed in the Solution Health of our CME Workbench.

Environment variables:
* SONAR_QUBE_TOKEN
* AUTHORIZATION_URL
* COGNITO_USERNAME
* COGNITO_PASSWORD
* jwk_url
* jwk_kid
* jwk_alg

The REST API can be reached using `/api/v1/quality-adapter` as base Uri. Available endpoints as follows:

```
# request the quality gate status of a service for a specific quality category
GET /status/{qualityCategory}/{microservices}

# request quality index for services
GET /qualityIndex/{microservices}
```

Note: The `{microservices}` parameters accepts a comma-separated list of service names. Currently available quality categories are `CodeQuality` and `Builds`.