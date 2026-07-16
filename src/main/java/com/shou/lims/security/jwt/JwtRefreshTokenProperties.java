package com.shou.lims.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Min;

@Validated
@ConfigurationProperties(prefix = "jwt.refresh-token")
public record JwtRefreshTokenProperties(@Min(1) Integer expiration) {}
