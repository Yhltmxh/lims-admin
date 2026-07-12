package com.shou.lims.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt.access-token")
public record JwtAccessTokenProperties(String secret, Integer expiration) {}
