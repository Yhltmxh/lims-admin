package com.shou.lims.security.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.shou.lims.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtTokenService {

    private static final String ISSUER = "lims-admin";
    private static final String AUDIENCE = "lims-web";

    private final JwtAccessTokenProperties accessTokenProperties;
    private final JwtRefreshTokenProperties refreshTokenProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateAccessToken(Long userId, String username, List<String> permissions) {
        return generateAccessToken(userId, username, permissions, 0);
    }

    public String generateAccessToken(Long userId, String username, List<String> permissions, Integer authVersion) {
        Algorithm algorithm = Algorithm.HMAC256(accessTokenProperties.secret());
        Instant now = Instant.now();
        return JWT.create()
                .withIssuer(ISSUER)
                .withAudience(AUDIENCE)
                .withSubject(String.valueOf(userId))
                .withJWTId(UUID.randomUUID().toString())
                .withClaim("username", username)
                .withClaim("permissions", String.join(",", permissions != null ? permissions : List.of()))
                .withClaim("authVersion", authVersion == null ? 0 : authVersion)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusSeconds(accessTokenProperties.expiration() * 60L)))
                .sign(algorithm);
    }

    public DecodedJWT verifyAccessToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(accessTokenProperties.secret());
            return JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .withAudience(AUDIENCE)
                    .build()
                    .verify(token);
        } catch (JWTVerificationException e) {
            throw new UnauthorizedException();
        }
    }

    public Long extractVerifiedUserIdForRefresh(String token) {
        return Long.valueOf(verifyForRefresh(token).getSubject());
    }

    public Integer extractVerifiedAuthVersionForRefresh(String token) {
        Integer version = verifyForRefresh(token).getClaim("authVersion").asInt();
        if (version == null) {
            throw new UnauthorizedException();
        }
        return version;
    }

    private DecodedJWT verifyForRefresh(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(accessTokenProperties.secret());
            long refreshWindowSeconds = refreshTokenProperties.expiration() * 24L * 60L * 60L;
            return JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .withAudience(AUDIENCE)
                    .acceptExpiresAt(refreshWindowSeconds)
                    .build()
                    .verify(token);
        } catch (JWTVerificationException e) {
            throw new UnauthorizedException();
        }
    }

    public long getRemainingSeconds(String token) {
        DecodedJWT jwt = verifyAccessToken(token);
        return Math.max(0L, jwt.getExpiresAtAsInstant().getEpochSecond() - Instant.now().getEpochSecond());
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenProperties.expiration() * 60L;
    }

    public String generateRefreshToken() {
        byte[] bytes = new byte[32]; // 256 bits
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
