package com.consort.security;

import com.consort.Errors;
import com.consort.QualityAdapterException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.pac4j.core.http.HttpActionAdapter;
import org.pac4j.sparkjava.SparkWebContext;
import spark.Spark;

@Slf4j
public class QualityHttpActionAdapter implements HttpActionAdapter<Object, SparkWebContext> {

    @Override
    public Object adapt(int code, SparkWebContext context) {

        final ObjectMapper objectMapper = new ObjectMapper();

        try {
            if (code == 401) {
                Spark.halt(401, objectMapper.writeValueAsString(
                        new QualityAdapterException(Errors.ERR_AUTH_FORBIDDEN, "QualityHttpActionAdapter::adapt")));
            } else if (code == 403) {
                Spark.halt(403, objectMapper.writeValueAsString(
                        new QualityAdapterException(Errors.ERR_AUTH_REQUIRED, "QualityHttpActionAdapter::adapt")));
            } else if (code == 200) {
                Spark.halt(200, context.getSparkResponse().body());
            } else if (code == 302) {
                context.getSparkResponse().redirect(context.getLocation());
            }
        } catch (JsonProcessingException e) {
            // Fallback routine in case sophisticated error handling cant be done
            log.error(
                    "CRITICAL ERROR when trying to process Exception. Can't provide proper Response sending nothing in body.");

            if (code == 401) {
                Spark.halt(401, Errors.ERR_AUTH_FORBIDDEN.getMessage());
            } else if (code == 403) {
                Spark.halt(403, Errors.ERR_AUTH_REQUIRED.getMessage());
            } else if (code == 200) {
                Spark.halt(200, context.getSparkResponse().body());
            } else if (code == 302) {
                context.getSparkResponse().redirect(context.getLocation());
            }
        }
        return null;
    }
}
