package com.tank.server.properties;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "settings")
@SuppressWarnings("fb-contrib:USBR_UNNECESSARY_STORE_BEFORE_RETURN")
@EqualsAndHashCode
public class ServerSettings {

    private String levelFile;

    private int tankEnergy;
    private int tankMovementDelay;
    private int tankShootDelay;
    
    private Long explosionTimeMilliseconds;
    private Long laserTimeMilliseconds;

    private String resetSecret;
}
