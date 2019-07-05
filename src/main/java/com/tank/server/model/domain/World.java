package com.tank.server.model.domain;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@RequiredArgsConstructor
public class World {

    private final List<Laser> lasers;
    private final List<Tank> tanks;
    private final Dimension dimensions;
    private final List<StaticObject> staticObjects;
}
