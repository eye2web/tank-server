package com.tank.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tank.server.model.domain.TankAction;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.lang.NonNull;

import java.util.UUID;

@Data
@NoArgsConstructor
public class ActionRequest {

    @NonNull
    @JsonProperty("tankControlId")
    private UUID controlId;

    @NonNull
    @JsonProperty("command")
    private TankAction action;
}
