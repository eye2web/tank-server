package com.tank.server.model.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum CardinalDirection {
    NORTH("north"), EAST("east"), SOUTH("south"), WEST("west");


    private final String direction;


    CardinalDirection(final String direction) {
        this.direction = direction;
    }

    @JsonValue
    public String getDirection() {
        return direction;
    }

    @JsonCreator
    public static CardinalDirection fromDirection(final String direction) {

        return Arrays.stream(CardinalDirection.values())
            .filter(val -> val.getDirection().equals(direction))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(direction));
    }

}
