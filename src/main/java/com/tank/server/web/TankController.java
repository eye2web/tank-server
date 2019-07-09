package com.tank.server.web;

import com.tank.server.model.ActionRequest;
import com.tank.server.model.SubscribeResponse;
import com.tank.server.model.SubscribeTankRequest;
import com.tank.server.model.domain.Tank;
import com.tank.server.service.StateService;

import lombok.RequiredArgsConstructor;

import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequiredArgsConstructor
public class TankController {
    private final StateService stateService;
    private final ModelMapper modelMapper;

    @PostMapping("/subscribe")
    public ResponseEntity<SubscribeResponse> subscribeTank(
        @Validated @RequestBody SubscribeTankRequest subscribeTankRequest) {

        if (!stateService.isTankAvailable()) {
            return ResponseEntity.badRequest().build();
        }

        final Tank tank;

        if (Objects.nonNull(subscribeTankRequest.getTankControlId())) {
            tank = stateService.joinGame(subscribeTankRequest.getName(), subscribeTankRequest.getTankControlId());
        } else {
            tank = stateService.joinGame(subscribeTankRequest.getName());
        }

        return ResponseEntity.ok(modelMapper.map(tank, SubscribeResponse.class));
    }

    @PostMapping("/tank")
    public ResponseEntity<Boolean> doTankAction(@Validated @RequestBody ActionRequest actionRequest) {

        return ResponseEntity.ok(stateService.tankAction(actionRequest));
    }

}
