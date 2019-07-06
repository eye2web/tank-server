package com.tank.server.web;

import com.tank.server.model.WorldResetRequest;
import com.tank.server.model.domain.World;
import com.tank.server.properties.ServerSettings;
import com.tank.server.service.StateService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class WorldController {

    private final ServerSettings serverSettings;
    private final StateService stateService;

    @PostMapping("/reset")
    private ResponseEntity<Void> resetWorld(@Validated @RequestBody final WorldResetRequest worldResetRequest) {

        if (worldResetRequest.getSecret().equals(serverSettings.getResetSecret())) {
            stateService.resetGame();
            return ResponseEntity.status(HttpStatus.OK).build();
        }

        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/world")
    private ResponseEntity<World> getWorld() {

        return ResponseEntity.ok(stateService.getWorld());
    }

}
