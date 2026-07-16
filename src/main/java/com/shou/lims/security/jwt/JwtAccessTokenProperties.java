package com.shou.lims.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Validated
@ConfigurationProperties(prefix = "jwt.access-token")
public record JwtAccessTokenProperties(
        @NotBlank @Size(min = 32) String secret,
        @Min(1) Integer expiration) {}
