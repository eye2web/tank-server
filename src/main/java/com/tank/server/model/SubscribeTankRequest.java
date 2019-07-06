package com.tank.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.lang.NonNull;

import java.util.UUID;

@Data
@NoArgsConstructor
public class SubscribeTankRequest {
    @NonNull
    private String name;

    @JsonProperty("tankControlId")
    private UUID tankControlId;
}
