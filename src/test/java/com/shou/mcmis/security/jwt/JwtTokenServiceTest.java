package com.shou.mcmis.security.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.shou.mcmis.BaseSpringBootTest;
import com.shou.mcmis.common.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class JwtTokenServiceTest extends BaseSpringBootTest {

    @Autowired
    private JwtTokenService jwtTokenService;

    @Test
    void shouldGenerateAndVerifyAccessToken() {
        String token = jwtTokenService.generateAccessToken(1L, "admin",
                List.of("organize:user:list", "organize:menu:list"));

        DecodedJWT jwt = JWT.decode(token);
        assertThat(jwt.getSubject()).isEqualTo("1");
        assertThat(jwt.getClaim("username").asString()).isEqualTo("admin");
        assertThat(jwt.getClaim("permissions").asString()).contains("organize:user:list");
    }

    @Test
    void shouldGenerateTokenWithEmptyPermissions() {
        String token = jwtTokenService.generateAccessToken(1L, "admin", List.of());
        String perms = JWT.decode(token).getClaim("permissions").asString();
        assertThat(perms).isEmpty();
    }

    @Test
    void shouldGenerateUniqueAccessTokensWithinTheSameSecond() {
        String firstToken = jwtTokenService.generateAccessToken(1L, "admin", List.of());
        String secondToken = jwtTokenService.generateAccessToken(1L, "admin", List.of());

        assertThat(secondToken).isNotEqualTo(firstToken);
        assertThat(JWT.decode(firstToken).getId()).isNotBlank();
        assertThat(JWT.decode(secondToken).getId())
                .isNotEqualTo(JWT.decode(firstToken).getId());
    }

    @Test
    void shouldVerifyValidToken() {
        String token = jwtTokenService.generateAccessToken(1L, "admin", List.of());
        DecodedJWT jwt = jwtTokenService.verifyAccessToken(token);
        assertThat(jwt.getSubject()).isEqualTo("1");
    }

    @Test
    void shouldRejectTamperedToken() {
        String token = jwtTokenService.generateAccessToken(1L, "admin", List.of());
        String tampered = token.substring(0, token.length() - 4) + "XXXX";
        assertThatThrownBy(() -> jwtTokenService.verifyAccessToken(tampered))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void shouldGenerateRefreshToken256Bit() {
        String refreshToken = jwtTokenService.generateRefreshToken();
        assertThat(refreshToken).hasSize(64); // 32 bytes = 64 hex chars
    }

    @Test
    void shouldExtractVerifiedUserIdForRefresh() {
        String token = jwtTokenService.generateAccessToken(42L, "admin", List.of());
        assertThat(jwtTokenService.extractVerifiedUserIdForRefresh(token)).isEqualTo(42L);
    }
}
