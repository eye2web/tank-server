package com.tank.server.service;

import com.tank.server.model.ActionRequest;
import com.tank.server.model.domain.CardinalDirection;
import com.tank.server.model.domain.Color;
import com.tank.server.model.domain.Explosion;
import com.tank.server.model.domain.Laser;
import com.tank.server.model.domain.StaticObject;
import com.tank.server.model.domain.Tank;
import com.tank.server.model.domain.TankAction;
import com.tank.server.model.domain.World;
import com.tank.server.properties.ServerSettings;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.ArrayUtils;
import org.modelmapper.internal.Pair;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Stack;
import java.util.UUID;

@Service
@Slf4j
public class StateService {

    private final WorldLoader worldLoader;

    private final Random random;
    private final ServerSettings serverSettings;
    private final Stack<Tank> availableTanks;

    private World world;

    private StateService(final WorldLoader worldLoader, final ServerSettings serverSettings) {
        availableTanks = new Stack<>();
        this.worldLoader = worldLoader;
        this.serverSettings = serverSettings;
        random = new Random();
        resetGame();
    }

    public World getWorld() {
        checkLaserExpired(world.getLasers());
        checkExplosionExpired(world.getExplosions());
        return world;
    }

    public void resetGame() {

        world = this.worldLoader.loadLevel();

        for (Color color : Color.values()) {
            final var tank = new Tank();
            tank.setColor(color);
            availableTanks.push(tank);
        }
        log.info("Server reset");
    }

    public boolean isTankAvailable() {
        return !availableTanks.isEmpty();
    }

    public Tank joinGame(final String name) {
        return joinGame(name, UUID.randomUUID());
    }

    public Tank joinGame(final String name, final UUID uuid) {
        if (!isTankAvailable()) {
            return null;
        }

        final var tank = availableTanks.pop();
        tank.setId(UUID.randomUUID());
        tank.setControlId(uuid);
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
            return tankFire(tankActionRequest.getControlId());
        } else {
            return moveTank(tankActionRequest.getControlId(), action);
        }
    }

    private void addTankToWorld(final Tank tank) {

        tank.setPosition(getRandomSpawnPoint());
        tank.setDirection(getRandomDirection());
        tank.setEnergy(serverSettings.getTankEnergy());
        world.getTanks().add(tank);
    }

    private int[] getRandomSpawnPoint() {

        final var positions = world.getStartingPositions();
        final var spawnPosition = positions.get(random.nextInt(positions.size()));

        if (isPositionAvailable(spawnPosition)) {
            return spawnPosition;
        } else {
            return getRandomSpawnPoint();
        }
    }

    private CardinalDirection getRandomDirection() {

        final var directions = CardinalDirection.values();

        final var randomIndex = random.nextInt(directions.length);

        return directions[randomIndex];
    }

    private boolean moveTank(final UUID controlId, final TankAction action) {

        final var tankOpt = findTankByControlId(controlId);

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

    private boolean tankFire(final UUID controlId) {

        final var tankOpt = findTankByControlId(controlId);

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
            int[] startPosition = null;

            do {
                position = nextPosition(position, direction.getVector2D());

                if (Objects.isNull(startPosition)) {
                    startPosition = position;
                }

                final var optTankShot = getTankAtPosition(position);

                if (optTankShot.isPresent()) {

                    handleTankHit(tank, optTankShot.get());
                    break;
                }
                final var optStaticObjectShot = collisionWithStaticObject(position);

                if (optStaticObjectShot.isPresent()) {

                    handleStaticObjectHit(optStaticObjectShot.get());
                    break;
                }
                
            } while (
                position[0] < world.getDimensions().getWidth() &&
                    position[0] > 0 &&
                    position[1] < world.getDimensions().getHeight() &&
                    position[1] > 0
            );

            addLaserToWorld(startPosition, position, tank.getDirection());

            return true;
        }

        return false;
    }

    private Optional<Tank> findTankByControlId(final UUID controlId) {
        return world.getTanks().stream()
            .filter(tank -> tank.getControlId().equals(controlId)).findFirst();
    }

    private void addLaserToWorld(final int[] startPosition,
                                 final int[] endPosition,
                                 final CardinalDirection direction) {

        world.getLasers().add(new Laser(
            UUID.randomUUID(),
            ArrayUtils.clone(startPosition),
            ArrayUtils.clone(endPosition),
            Timestamp.from(Instant.now()),
            serverSettings.getLaserTimeMilliseconds(),
            direction
        ));
    }

    private void checkLaserExpired(final List<Laser> lasers) {

        final var deleteLasers = new ArrayList<Laser>();

        for (Laser laser : lasers) {

            if (laser.getStartTime().toInstant().plusMillis(laser.getDurationMilliseconds()).isBefore(Instant.now())) {
                deleteLasers.add(laser);
            }

        }

        lasers.removeAll(deleteLasers);
    }

    private void checkExplosionExpired(final List<Explosion> explosions) {

        final var deleteExplosions = new ArrayList<Explosion>();

        for (Explosion explosion : explosions) {

            if (explosion.getStartTime().toInstant().plusMillis(explosion.getDurationMilliseconds())
                .isBefore(Instant.now())) {
                deleteExplosions.add(explosion);
            }

        }

        explosions.removeAll(deleteExplosions);
    }

    private void handleTankHit(final Tank shootingTank, final Tank targetTank) {

        targetTank.setEnergy(targetTank.getEnergy() - 1);
        shootingTank.getHits().add(targetTank.getId());

        if (targetTank.getEnergy() < 1) {

            shootingTank.getKills().add(targetTank.getId());
            world.getTanks().remove(targetTank);
            createWorldExplosion("tank", targetTank.getId(), targetTank.getPosition());

            // Free tank to the pool
            addTankToPool(targetTank);
        }

    }

    private void handleStaticObjectHit(final StaticObject staticObject) {
        if (staticObject.getEnergy() > 0) {
            staticObject.setEnergy(staticObject.getEnergy() - 1);

            if (staticObject.getEnergy() < 1) {

                world.getStaticObjects().remove(staticObject);
                createWorldExplosion(staticObject.getType(), staticObject.getId(), staticObject.getPosition());
            }
        }
    }

    private void createWorldExplosion(final String type, final UUID id, final int[] position) {
        final var explosion = new Explosion();
        explosion.setId(UUID.randomUUID());
        explosion.setType(type);
        explosion.setEntityId(id);
        explosion.setStartPos(ArrayUtils.clone(position));
        explosion.setStartTime(Timestamp.from(Instant.now()));
        explosion.setDurationMilliseconds(serverSettings.getExplosionTimeMilliseconds());
        world.getExplosions().add(explosion);
    }

    private Optional<Tank> getTankAtPosition(final int[] position) {
        return world.getTanks().stream().filter(tank -> Arrays.equals(tank.getPosition(), position)).findFirst();
    }

    private Optional<StaticObject> collisionWithStaticObject(final int[] position) {
        return world.getStaticObjects().stream()
            .filter(staticObject -> Arrays.equals(staticObject.getPosition(), position))
            .findFirst();
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
