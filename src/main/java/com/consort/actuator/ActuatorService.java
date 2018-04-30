package com.consort.actuator;

import spark.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActuatorService {

    private static ActuatorService instance = null;
    private static final Map<String, ActuatorCounter> counterMap = new HashMap<>();
    private static final String COUNTER_A = "counterA";
    private static final String COUNTER_B = "counterB";


    public static ActuatorService getInstance() {
        if (instance == null) {
            instance = new ActuatorService();
        }

        return instance;
    }

    public List<ActuatorCounter> getCounters(final Response response) {

        response.type("application/json");

        if (counterMap.isEmpty()) {
            counterMap.put(COUNTER_A, new ActuatorCounter(1, COUNTER_A));
            counterMap.put(COUNTER_B, new ActuatorCounter(1, COUNTER_B));
        } else {
            final ActuatorCounter counterA = counterMap.get(COUNTER_A);
            final ActuatorCounter counterB = counterMap.get(COUNTER_B);
            counterA.setValue(counterA.getValue() + 1);
            counterA.setValue(counterB.getValue() + 1);
        }

        return getActuatorCounterList();
    }

    private List<ActuatorCounter> getActuatorCounterList() {
        List<ActuatorCounter> counterList = new ArrayList<>();

        for (Map.Entry<String, ActuatorCounter> entry : counterMap.entrySet()) {
            counterList.add(entry.getValue());
        }

        return counterList;
    }

    public ActuatorCounter getCounterByName(final String name) {

        final ActuatorCounter counter = counterMap.get(name);

        if (counter != null) {
            counter.setValue(counter.getValue() + 1);
        }

        return counter;
    }
}
