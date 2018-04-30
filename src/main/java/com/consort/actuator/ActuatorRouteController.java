package com.consort.actuator;

import com.fasterxml.jackson.databind.ObjectMapper;
import spark.Service;

public class ActuatorRouteController {

    public void initRoutes() {
        final Service http = Service.ignite().port(8081);
        http.get("/health", (req, res) -> {
            final ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(new ActuatorStatus("UP"));
        });

        http.get("/metrics", (req, res) -> {
            final ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(ActuatorService.getInstance().getCounters(res));
        });

        http.get("/metrics/:name", (req, res) -> {
            final ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(ActuatorService.getInstance().getCounterByName(req.params("name")));
        });
    }
}
