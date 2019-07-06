package com.tank.server.model.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaticObject {

    private UUID id;
    private String type;
    private int[] position;
    private int energy;
}
