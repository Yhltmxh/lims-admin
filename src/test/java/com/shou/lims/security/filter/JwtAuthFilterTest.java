package com.shou.lims.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.shou.lims.BaseSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class JwtAuthFilterTest extends BaseSpringBootTest {

    @Autowired
    private ObjectMapper objectMapper;

    private String tokenFor(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("username", username, "cipherPwd", password));
        String resp = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resp, "$.data.accessToken");
    }

    @Test
    void shouldAllowPublicEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/auth/public-key").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void shouldRejectProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/system/users").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowProtectedEndpointWithValidToken() throws Exception {
        String token = tokenFor("admin", "123456");
        mockMvc.perform(get("/system/users")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void shouldRejectExpiredToken() throws Exception {
        String expiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."
                + "eyJzdWIiOiIxIiwidXNlcm5hbWUiOiJhZG1pbiIsInBlcm1pc3Npb25zIjoiIiwiaWF0IjoxNzAwMDAwMDAwLCJleHAiOjE3MDAwMDAwMDF9."
                + "invalid_signature";
        mockMvc.perform(get("/system/users")
                        .header("Authorization", "Bearer " + expiredToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectUserWithoutRequiredPermission() throws Exception {
        String token = tokenFor("liming", "123456");
        // Authenticated user lacking the required authority: @PreAuthorize throws
        // AccessDeniedException during controller invocation, which is caught by the
        // @RestControllerAdvice GlobalExceptionHandler (inside the DispatcherServlet)
        // BEFORE it can reach the security filter's AccessDeniedHandler. The advice
        // returns HTTP 200 with a body whose code=403 ("无访问权限"). A valid request
        // body is required so the request passes @Valid validation and actually reaches
        // the authorization check (an invalid body would short-circuit with code=400).
        mockMvc.perform(post("/system/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"tester\",\"password\":\"123456\",\"realName\":\"Tester\",\"roleIds\":[1]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }
}
