package com.consort.quality;

import com.consort.Errors;
import com.consort.QualityAdapterException;
import com.consort.security.AuthorizationFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import spark.ExceptionHandler;
import spark.Request;
import spark.Response;
import spark.Service;

import static spark.Service.ignite;

@Slf4j
@AllArgsConstructor
public class QualityController {

    private static final String BASE_URI = "/api/v1/quality-adapter";

    private static final String AUTHORIZE_NAME = "scope";
    private static final String ROLE_ADMIN = "aws.cognito.signin.user.admin";

    private QualityService qualityService;

    public void initRoutes() {
        final Service http = ignite().port(8080);

        enableCORS(http, "*", "GET, POST", "Content-Type, Authorization");
        filterNotFoundAndInternalServerError(http);
        applyAuthorizationFilter(http);
        createRestApiRoutes(http);
        registerExceptionHandler(http);
    }

    private static void enableCORS(final Service http, final String origin, final String methods,
            final String headers) {
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

    private void filterNotFoundAndInternalServerError(Service http) {
        final ObjectMapper objectMapper = new ObjectMapper();
        http.notFound((req, res) -> objectMapper
                .writeValueAsString(new QualityAdapterException(Errors.ERR_NOT_FOUND, "InitRoutes: Not Found.")));
        http.internalServerError((request, response) -> objectMapper.writeValueAsString(
                new QualityAdapterException(Errors.ERR_UNKNOWN_ERROR, "InitRoutes: Unkown Error.")));
    }

    private void applyAuthorizationFilter(Service http) {
        http.before(BASE_URI + "/status/:category/:microservices", new AuthorizationFilter(AUTHORIZE_NAME, ROLE_ADMIN));
        http.before(BASE_URI + "/qualityIndex/:microservices", new AuthorizationFilter(AUTHORIZE_NAME, ROLE_ADMIN));
        http.before(BASE_URI + "/code-quality/:microservices", new AuthorizationFilter(AUTHORIZE_NAME, ROLE_ADMIN));
    }

    private void createRestApiRoutes(Service http) {
        http.get(BASE_URI + "/status/:category/:microservices", QualityService::getQualityInformation);
        http.get(BASE_URI + "/qualityIndex/:microservices", QualityService::getQualityIndex);
        http.get(BASE_URI + "/code-quality/:microservices", qualityService::getCumulatedCodeQualityDetails);
    }

    private void registerExceptionHandler(final Service http) {
        http.exception(QualityAdapterException.class, (exception, request, response) -> {
            response.status(exception.getStatus());
            response.type("application/json");
            try {
                response.body(new ObjectMapper().writeValueAsString(exception));
            } catch (JsonProcessingException e) {
                response.status(500);
                log.error("Error in ErrorHandler", e);
            }
        });
    }

}
