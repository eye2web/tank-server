package com.tank.server.web;

import com.tank.server.model.domain.World;
import com.tank.server.service.StateService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class WorldController {

    private final StateService stateService;

    @GetMapping("/reset")
    private ResponseEntity<Void> resetWorld() {

        stateService.resetGame();

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/world")
    private ResponseEntity<World> getWorld() {
        
        return ResponseEntity.ok(stateService.getWorld());
    }

}
