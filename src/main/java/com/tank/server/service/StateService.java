package com.tank.server.service;

import com.tank.server.factory.StaticObjectFactory;
import com.tank.server.model.ActionRequest;
import com.tank.server.model.domain.CardinalDirection;
import com.tank.server.model.domain.Color;
import com.tank.server.model.domain.Dimension;
import com.tank.server.model.domain.StaticObject;
import com.tank.server.model.domain.Tank;
import com.tank.server.model.domain.TankAction;
import com.tank.server.model.domain.World;
import com.tank.server.properties.ServerSettings;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.ArrayUtils;
import org.modelmapper.internal.Pair;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Stack;

@Service
@Slf4j
public class StateService {

    private final Random random;
    private final ServerSettings serverSettings;
    private final StaticObjectFactory staticObjectFactory;
    private Stack<Tank> availableTanks;

    @Getter
    private World world;

    private StateService(final ServerSettings serverSettings, final StaticObjectFactory staticObjectFactory) {
        this.serverSettings = serverSettings;
        this.staticObjectFactory = staticObjectFactory;
        random = new Random();
        resetGame();
    }

    public void resetGame() {

        world =
            new World(
                new ArrayList<>(),
                new Dimension(serverSettings.getLevelWidth(), serverSettings.getLevelHeight()),
                getStaticWorldObjects());
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

    public boolean tankAction(final ActionRequest tankActionRequest) {

        final var action = tankActionRequest.getAction();

        if (action.equals(TankAction.FIRE)) {
            return tankFire(tankActionRequest.getTankId());
        } else {
            return moveTank(tankActionRequest.getTankId(), action);
        }
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

            // Check for timed delay
            if (tank.getLastMove() != null &&
                tank.getLastMove().toInstant()
                    .plusSeconds(serverSettings.getTankMovementDelay())
                    .isAfter(Instant.now())) {
                return false;
            }

            final var position = nextPosition(tank.getPosition(), action.getVector2D());

            if (isPositionAvailable(position)) {
                tank.setPosition(position);
                tank.setDirection(CardinalDirection.fromDirection(action.getAction()));
                tank.setLastMove(Timestamp.from(Instant.now()));
            }
            return true;
        }

        return false;
    }

    private int[] nextPosition(final int[] position, Pair<Integer, Integer> vector2D) {
        final var pos = ArrayUtils.clone(position);
        pos[0] = pos[0] + vector2D.getLeft();
        pos[1] = pos[1] + vector2D.getRight();
        return pos;
    }

    private boolean tankFire(final int tankId) {

        final var tankOpt = world.getTanks().stream()
            .filter(tank -> tank.getId() == tankId).findFirst();

        if (tankOpt.isPresent()) {
            final var tank = tankOpt.get();

            // Check for timed delay
            if (tank.getLastShot() != null &&
                tank.getLastShot().toInstant()
                    .plusSeconds(serverSettings.getTankShootDelay())
                    .isAfter(Instant.now())) {
                return false;
            }
            tank.setLastShot(Timestamp.from(Instant.now()));

            final var direction = TankAction.fromAction(tank.getDirection().getDirection());

            int[] position = tank.getPosition();

            do {
                position = nextPosition(position, direction.getVector2D());

                final var optTankShot = getTankAtPosition(position);

                if (optTankShot.isPresent()) {

                    handleTankHit(tank, optTankShot.get());
                    return true;
                }
                final var optStaticObjectShot = collisionWithStaticObject(position);

                if (optStaticObjectShot.isPresent()) {

                    handleStaticObjectHit(optStaticObjectShot.get());
                    return true;
                }

            } while (
                position[0] < serverSettings.getLevelWidth() &&
                    position[0] > 0 &&
                    position[1] < serverSettings.getLevelHeight() &&
                    position[1] > 0
            );

        }

        return false;
    }

    private void handleTankHit(final Tank shootingTank, final Tank targetTank) {

        targetTank.setEnergy(targetTank.getEnergy() - 1);
        shootingTank.getHits().add(targetTank.getId());

        if (targetTank.getEnergy() < 1) {

            shootingTank.getKills().add(targetTank.getId());
            world.getTanks().remove(targetTank);

            // Free tank to the pool
            addTankToPool(targetTank);
        }

    }

    private void handleStaticObjectHit(final StaticObject staticObject) {
        if (staticObject.getEnergy() > 0) {
            staticObject.setEnergy(staticObject.getEnergy() - 1);

            if (staticObject.getEnergy() < 1) {

                world.getStaticObjects().remove(staticObject);
            }
        }
    }

    private Optional<Tank> getTankAtPosition(final int[] position) {
        return world.getTanks().stream().filter(tank -> Arrays.equals(tank.getPosition(), position)).findFirst();
    }

    private Optional<StaticObject> collisionWithStaticObject(final int[] position) {
        return world.getStaticObjects().stream()
            .filter(staticObject -> Arrays.equals(staticObject.getPosition(), position))
            .findFirst();
    }

    private List<StaticObject> getStaticWorldObjects() {

        final var staticObjects = new ArrayList<StaticObject>();

        placeWalls(staticObjects);
        placeTrees(staticObjects);

        return staticObjects;
    }

    private void placeWalls(List<StaticObject> staticObjects) {

        for (int x = 0; x < serverSettings.getLevelWidth(); x++) {
            for (int y = 0; y < serverSettings.getLevelHeight(); y++) {

                final int[] position = new int[] {x, y};

                if (x == 0 ||
                    x == serverSettings.getLevelWidth() - 1 ||
                    y == 0 ||
                    y == serverSettings.getLevelHeight() - 1
                ) {
                    staticObjects.add(
                        staticObjectFactory.createStaticObject(position, StaticObjectFactory.StaticObjectType.WALL)
                    );
                }


            }
        }
    }

    private void placeTrees(List<StaticObject> staticObjects) {

        for (int x = 0; x < serverSettings.getLevelWidth(); x++) {
            for (int y = 0; y < serverSettings.getLevelHeight(); y++) {

                final int[] position = new int[] {x, y};

                if (
                    (x > 3 && x < serverSettings.getLevelWidth() - 4 && (y == 3
                        || y == serverSettings.getLevelHeight() - 4)) ||

                        y > 3 && y < serverSettings.getLevelHeight() - 4 && (x == 3
                            || x == serverSettings.getLevelWidth() - 4)
                ) {
                    staticObjects.add(
                        staticObjectFactory.createStaticObject(position, StaticObjectFactory.StaticObjectType.TREE)
                    );
                }
            }
        }
    }

    private boolean isPositionAvailable(final int[] position) {

        return world.getStaticObjects().stream()
            .noneMatch(staticObject -> Arrays.equals(staticObject.getPosition(), position)) &&
            world.getTanks().stream()
                .noneMatch(tank -> Arrays.equals(tank.getPosition(), position));
    }

    private void addTankToPool(final Tank tank) {

        tank.setLastMove(null);
        tank.setLastShot(null);
        tank.setReloaded(null);

        availableTanks.push(tank);
    }
}
