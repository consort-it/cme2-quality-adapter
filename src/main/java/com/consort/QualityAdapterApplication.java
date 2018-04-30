package com.consort;

import com.consort.actuator.ActuatorRouteController;
import com.consort.quality.QualityController;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QualityAdapterApplication {

    public static void main(String[] args) {

        log.info("Quality Adapter started!");

        // quality adapter
        QualityController qualityController = new QualityController();
        qualityController.initRoutes();

        // actuator
        ActuatorRouteController routeController = new ActuatorRouteController();
        routeController.initRoutes();
    }
}
