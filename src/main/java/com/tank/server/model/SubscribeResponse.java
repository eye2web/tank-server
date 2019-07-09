package com.tank.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class SubscribeResponse {

    @JsonProperty("tankId")
    private UUID id;

    @JsonProperty("tankControlId")
    private UUID controlId;

}
