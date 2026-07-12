package com.shou.lims.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt.refresh-token")
public record JwtRefreshTokenProperties(Integer expiration) {}
