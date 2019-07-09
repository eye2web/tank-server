package com.tank.server.service;

import com.tank.server.factory.StaticObjectFactory;
import com.tank.server.model.domain.Dimension;
import com.tank.server.model.domain.StaticObject;
import com.tank.server.model.domain.World;
import com.tank.server.properties.ServerSettings;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class WorldLoader {

    private final StaticObjectFactory staticObjectFactory;
    private final ServerSettings serverSettings;
    private final ResourceLoader resourceLoader;

    public World loadLevel() {

        final var lines = getFileLines();
        int[] dimensions = getLevelDimensions(lines);

        return
            new World(
                UUID.randomUUID(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new Dimension(dimensions[0], dimensions[1]),
                getStaticObjects(lines),
                getStartingPositions(lines));
    }

    private List<StaticObject> getStaticObjects(final List<String> lines) {

        final var staticObjects = new ArrayList<StaticObject>();

        for (int y = 0; y < lines.size(); y++) {

            final var line = lines.get(y);
            for (int x = 0; x < line.length(); x++) {

                switch (line.charAt(x)) {
                    case 'w':
                        staticObjects.add(
                            staticObjectFactory.createStaticObject(new int[] {x, y},
                                StaticObjectFactory.StaticObjectType.WALL));
                        break;
                    case 't':
                        staticObjects.add(
                            staticObjectFactory.createStaticObject(new int[] {x, y},
                                StaticObjectFactory.StaticObjectType.TREE));
                        break;
                }
            }
        }

        return staticObjects;
    }

    private List<int[]> getStartingPositions(final List<String> lines) {

        final var startingPositions = new ArrayList<int[]>();

        for (int y = 0; y < lines.size(); y++) {

            final var line = lines.get(y);
            for (int x = 0; x < line.length(); x++) {

                if (line.charAt(x) == 's') {
                    startingPositions.add(new int[] {x, y});
                }
            }
        }

        return startingPositions;
    }

    private int[] getLevelDimensions(final List<String> lines) {

        final var levelWidth = lines.get(0).length();
        final var levelHeight = lines.size();

        return new int[] {levelWidth, levelHeight};
    }


    private List<String> getFileLines() {
        final var lines = new ArrayList<String>();

        final var resource = resourceLoader.getResource("classpath:levels/" + serverSettings.getLevelFile());
        try {

            final var reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));

            String line;
            while (Objects.nonNull(line = reader.readLine())) {
                lines.add(line);
            }

            reader.close();
        } catch (IOException iex) {
            log.error("Level load: {}", iex.getMessage());
        }

        return lines;
    }

}
