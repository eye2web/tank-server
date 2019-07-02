package com.tank.server.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.lang.NonNull;

@Data
@NoArgsConstructor
public class SubscribeTankRequest {
    @NonNull
    private String name;
}
