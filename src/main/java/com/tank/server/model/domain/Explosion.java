package com.tank.server.model.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.UUID;

@Data
@NoArgsConstructor
public class Explosion {

    private UUID id;

    private UUID entityId;
    private String type;

    private int[] startPos;
    private Timestamp startTime;

    private Long durationMilliseconds;
}
