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

@Component
@RequiredArgsConstructor
public class JwtTokenService {

    private final JwtAccessTokenProperties accessTokenProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateAccessToken(Long userId, String username, List<String> permissions) {
        Algorithm algorithm = Algorithm.HMAC256(accessTokenProperties.secret());
        Instant now = Instant.now();
        return JWT.create()
                .withSubject(String.valueOf(userId))
                .withClaim("username", username)
                .withClaim("permissions", String.join(",", permissions != null ? permissions : List.of()))
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusSeconds(accessTokenProperties.expiration() * 60L)))
                .sign(algorithm);
    }

    public DecodedJWT verifyAccessToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(accessTokenProperties.secret());
            return JWT.require(algorithm).build().verify(token);
        } catch (JWTVerificationException e) {
            throw new UnauthorizedException();
        }
    }

    public String generateRefreshToken() {
        byte[] bytes = new byte[32]; // 256 bits
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
