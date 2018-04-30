package com.consort.quality;

import com.consort.Errors;
import com.consort.QualityAdapterException;
import com.consort.security.AuthorizationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import spark.Service;

import static spark.Service.ignite;

@Slf4j
public class QualityController {

    private static final String BASE_URI = "/api/v1/quality-adapter";

    private static final String AUTHORIZE_NAME = "scope";
    private static final String ROLE_ADMIN = "aws.cognito.signin.user.admin";

    public void initRoutes() {

        final Service http = ignite().port(8080);

        enableCORS(http, "*", "GET, POST", "Content-Type, Authorization");

        filterNotFoundAndInternalServerError(http);

        applyAuthorizationFilter(http);
        createRestApiRoutes(http);
    }

    private void createRestApiRoutes(Service http) {
        http.get(BASE_URI + "/:category/:microservices", QualityService::getQualityInformation);
    }

    private void applyAuthorizationFilter(Service http) {
        http.before(BASE_URI + "/:category/:microservices", new AuthorizationFilter(AUTHORIZE_NAME, ROLE_ADMIN));
    }

    private void filterNotFoundAndInternalServerError(Service http) {
        final ObjectMapper objectMapper = new ObjectMapper();
        http.notFound((req, res) -> objectMapper.writeValueAsString(new QualityAdapterException(Errors.ERR_NOT_FOUND, "InitRoutes: Not Found.")));
        http.internalServerError((request, response) -> objectMapper.writeValueAsString(new QualityAdapterException(Errors.ERR_UNKNOWN_ERROR, "InitRoutes: Unkown Error.")));
    }

    private static void enableCORS(final Service http, final String origin, final String methods, final String headers) {

        http.options("/*", (req, res) -> {

            final String acRequestHeaders = req.headers("Access-Control-Request-Headers");
            if (acRequestHeaders != null) {
                res.header("Access-Control-Allow-Headers", acRequestHeaders);
            }

            final String accessControlRequestMethod = req.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                res.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }

            return "OK";
        });

        http.before((req, res) -> {
            res.header("Access-Control-Allow-Origin", origin);
            res.header("Access-Control-Request-Method", methods);
            res.header("Access-Control-Allow-Headers", headers);
            res.type("application/json");
            res.header("Server", "-");
        });
    }
}
