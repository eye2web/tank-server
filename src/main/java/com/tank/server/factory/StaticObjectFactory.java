package com.tank.server.factory;

import com.tank.server.model.domain.StaticObject;

import lombok.Getter;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class StaticObjectFactory {

    public enum StaticObjectType {
        WALL("wall", -1), TREE("tree", 1);

        @Getter
        private final String type;

        @Getter
        private final int energy;

        StaticObjectType(final String type, final int energy) {
            this.type = type;
            this.energy = energy;
        }

    }

    public StaticObject createStaticObject(final int[] position, final StaticObjectType type) {

        return new StaticObject(UUID.randomUUID(), type.getType(), position, type.getEnergy());
    }

}
