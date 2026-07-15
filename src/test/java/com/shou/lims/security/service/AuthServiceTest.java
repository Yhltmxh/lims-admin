package com.shou.lims.security.service;

import com.shou.lims.BaseSpringBootTest;
import com.shou.lims.common.exception.UnauthorizedException;
import com.shou.lims.security.vo.LoginVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class AuthServiceTest extends BaseSpringBootTest {

    @Autowired
    private AuthService authService;

    @Test
    void shouldLoginSuccessfully() {
        LoginVO result = authService.login("admin", "123456");
        assertThat(result.getAccessToken()).isNotBlank();
        assertThat(result.getRefreshToken()).isNotBlank();
        assertThat(result.getExpiresIn()).isEqualTo(900L);
    }

    @Test
    void shouldFailLoginWithWrongPassword() {
        assertThatThrownBy(() -> authService.login("admin", "wrong"))
                .isInstanceOf(org.springframework.security.core.AuthenticationException.class);
    }

    @Test
    void shouldFailLoginWithNonexistentUser() {
        assertThatThrownBy(() -> authService.login("nobody", "123456"))
                .isInstanceOf(org.springframework.security.core.AuthenticationException.class);
    }

    @Test
    void shouldRefreshAndRotateToken() {
        LoginVO loginResult = authService.login("admin", "123456");
        LoginVO refreshResult = authService.refresh(
                loginResult.getAccessToken(), loginResult.getRefreshToken());

        assertThat(refreshResult.getAccessToken()).isNotBlank();
        assertThat(refreshResult.getRefreshToken()).isNotBlank();
        assertThatThrownBy(() -> authService.refresh(
                loginResult.getAccessToken(), loginResult.getRefreshToken()))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void shouldRejectWrongRefreshToken() {
        LoginVO loginResult = authService.login("admin", "123456");
        assertThatThrownBy(() -> authService.refresh(
                loginResult.getAccessToken(), "wrong-refresh-token"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void shouldLogoutAndRevokeRefreshToken() {
        LoginVO loginResult = authService.login("admin", "123456");
        SecurityUserDetails userDetails = new SecurityUserDetails(
                1L, "admin", "", true, List.of(new SimpleGrantedAuthority("organize:user")));
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        authService.logout(loginResult.getAccessToken());

        assertThatThrownBy(() -> authService.refresh(
                loginResult.getAccessToken(), loginResult.getRefreshToken()))
                .isInstanceOf(UnauthorizedException.class);
    }
}
