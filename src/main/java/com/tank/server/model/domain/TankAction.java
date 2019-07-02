package com.tank.server.model.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import org.modelmapper.internal.Pair;

import java.util.Arrays;

public enum TankAction {
    NORTH("north", Pair.of(0, -1)),
    EAST("east", Pair.of(1, 0)),
    SOUTH("south", Pair.of(0, 1)),
    WEST("west", Pair.of(-1, 0)),
    FIRE("fire", null);

    private final String action;
    private final Pair<Integer, Integer> vector2D;

    TankAction(final String action, Pair<Integer, Integer> vector2D) {
        this.action = action;
        this.vector2D = vector2D;
    }

    public Pair<Integer, Integer> getVector2D() {
        return vector2D;
    }

    @JsonValue
    public String getAction() {
        return action;
    }

    @JsonCreator
    public static TankAction fromAction(final String action) {

        return Arrays.stream(TankAction.values())
            .filter(val -> val.getAction().equals(action))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(action));
    }
}
