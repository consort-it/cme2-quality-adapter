package com.consort.security;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import spark.utils.StringUtils;

@Slf4j
public class EnvironmentContext {

    private static EnvironmentContext context = null;
    private Dotenv dotenv = null;

    private EnvironmentContext() {
        initEnvironment();
    }

    public static EnvironmentContext getInstance() {
        if (context == null) {
            context = new EnvironmentContext();
        }

        return context;
    }

    private void initEnvironment() {
        try {
            dotenv = Dotenv.configure().load();
        } catch (Exception e) {
            log.info("Dotenv configuration failed! Ignore if running on prod!");
        }
    }

    public String getenv(final String propertyName) {
        if (dotenv != null) {
            return dotenv.get(propertyName);
        } else {
            final String systemProperty = System.getenv(propertyName);
            if (StringUtils.isBlank(systemProperty)) {
                log.warn("Could not find system property -> " + systemProperty + "!");
            }
            return systemProperty;
        }
    }
}
