package com.tank.server.model.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class Tank {

    private int id;
    private List<Integer> kills = new ArrayList<>();
    private List<Integer> hits = new ArrayList<>();

    private int[] position = new int[2];

    private int energy;

    @JsonProperty("last-move")
    private Timestamp lastMove;
    @JsonProperty("last-shot")
    private Timestamp lastShot;
    private Timestamp reloaded;

    private Color color;

    private String name;

    @JsonProperty("orientation")
    private CardinalDirection direction;
}
