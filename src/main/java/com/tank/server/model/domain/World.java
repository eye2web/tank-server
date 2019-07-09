package com.tank.server.model.domain;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@RequiredArgsConstructor
public class World {

    private final UUID id;
    private final List<Explosion> explosions;
    private final List<Laser> lasers;
    private final List<Tank> tanks;
    private final Dimension dimensions;
    private final List<StaticObject> staticObjects;
}
