package com.tank.server.model.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Laser {

    private UUID id;
    private int[] startPos;
    private int[] endPos;

    private Timestamp startTime;
    private Timestamp endTime;

    private CardinalDirection direction;
}
