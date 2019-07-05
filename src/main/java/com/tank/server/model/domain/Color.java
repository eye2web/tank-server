package com.tank.server.model.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum Color {
    RED("red"), BLUE("greenyellow"), PINK("hotpink"), ORANGE("orange");

    private final String color;

    Color(final String color) {
        this.color = color;
    }

    @JsonValue
    public String getColor() {
        return color;
    }

    @JsonCreator
    public static Color fromDirection(final String color) {

        return Arrays.stream(Color.values())
            .filter(val -> val.getColor().equals(color))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(color));
    }

}
