package com.tank.server.service;

import com.tank.server.model.ActionRequest;
import com.tank.server.model.domain.CardinalDirection;
import com.tank.server.model.domain.Color;
import com.tank.server.model.domain.Dimension;
import com.tank.server.model.domain.Tank;
import com.tank.server.model.domain.TankAction;
import com.tank.server.model.domain.World;
import com.tank.server.properties.ServerSettings;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Random;
import java.util.Stack;

@Service
@Slf4j
public class StateService {

    private final Random random;
    private final ServerSettings serverSettings;
    private Stack<Tank> availableTanks;

    @Getter
    private World world;

    private StateService(final ServerSettings serverSettings) {
        this.serverSettings = serverSettings;
        random = new Random();
        resetGame();
    }

    public void resetGame() {
        world =
            new World(
                new ArrayList<>(),
                new Dimension(serverSettings.getLevelWidth(), serverSettings.getLevelHeight()));
        availableTanks = new Stack<>();

        int id = Color.values().length;

        for (Color color : Color.values()) {
            final var tank = new Tank();
            tank.setId(id);
            tank.setColor(color);
            availableTanks.push(tank);
            id--;
        }

        log.info("Server reset");
    }

    public boolean isTankAvailable() {
        return !availableTanks.isEmpty();
    }

    public Tank joinGame(final String name) {
        if (!isTankAvailable()) {
            return null;
        }

        final var tank = availableTanks.pop();
        tank.setName(name);

        log.info(String
            .format("Tank with name %s, id %s and color %s added", tank.getName(), tank.getId(),
                tank.getColor().getColor()));

        log.info(String
            .format("Available tanks: %s", availableTanks.size()));

        addTankToWorld(tank);
        return tank;
    }

    private void addTankToWorld(final Tank tank) {

        tank.setPosition(getRandomSpawnPoint());
        tank.setDirection(getRandomDirection());
        tank.setEnergy(serverSettings.getTankEnergy());
        world.getTanks().add(tank);
    }

    private int[] getRandomSpawnPoint() {

        final int margin = 2;
        final int beginWidth = serverSettings.getLevelWidth() / 2 - margin;
        final int endWidth = serverSettings.getLevelWidth() / 2 + margin + 1;

        final int beginHeight = serverSettings.getLevelHeight() / 2 - margin;
        final int endHeight = serverSettings.getLevelHeight() / 2 + margin + 1;

        final var side = getRandomDirection();

        switch (side) {
            case NORTH:
                return new int[] {random.ints(beginWidth, endWidth).findFirst().getAsInt(), 1};
            case SOUTH:
                return new int[] {random.ints(beginWidth, endWidth).findFirst().getAsInt(),
                    serverSettings.getLevelHeight() - 1};
            case WEST:
                return new int[] {1, random.ints(beginHeight, endHeight).findFirst().getAsInt()};
            case EAST:
                return new int[] {serverSettings.getLevelWidth() - 1,
                    random.ints(beginHeight, endHeight).findFirst().getAsInt()};
        }
        return new int[] {0, 0};
    }

    private CardinalDirection getRandomDirection() {

        final var directions = CardinalDirection.values();

        final var randomIndex = random.nextInt(directions.length);

        return directions[randomIndex];
    }

    private boolean moveTank(final int tankId, final TankAction action) {

        final var tankOpt = world.getTanks().stream()
            .filter(tank -> tank.getId() == tankId).findFirst();

        if (tankOpt.isPresent()) {

            final var tank = tankOpt.get();
            final var vector2D = action.getVector2D();
            final var position = ArrayUtils.clone(tank.getPosition());

            position[0] = position[0] + vector2D.getLeft();
            position[1] = position[1] + vector2D.getRight();

            // TODO validate if movement is possible

            // Check for timed delay
            if (tank.getLastMove() != null &&
                tank.getLastMove().toInstant()
                    .plusSeconds(serverSettings.getTankMovementDelay())
                    .isAfter(Instant.now())) {
                return false;
            }

            tank.setPosition(position);
            tank.setDirection(CardinalDirection.fromDirection(action.getAction()));
            tank.setLastMove(Timestamp.from(Instant.now()));

            return true;
        }

        return false;
    }

    private boolean tankFire(final int tankId) {

        // TODO implement tank fire
        return false;
    }

    public boolean tankAction(final ActionRequest tankActionRequest) {

        final var action = tankActionRequest.getAction();

        if (action.equals(TankAction.FIRE)) {
            return tankFire(tankActionRequest.getTankId());
        } else {
            return moveTank(tankActionRequest.getTankId(), action);
        }
    }

}
