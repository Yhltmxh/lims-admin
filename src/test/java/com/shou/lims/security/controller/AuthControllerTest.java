package com.shou.lims.security.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.shou.lims.BaseSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends BaseSpringBootTest {

    @Autowired
    private ObjectMapper objectMapper;

    private String loginAsAdmin() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("username", "admin", "cipherPwd", "123456"));
        String resp = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resp, "$.data.accessToken");
    }

    @Test
    void shouldLogin() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("username", "admin", "cipherPwd", "123456"));
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    void shouldFailLoginWithWrongPassword() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("username", "admin", "cipherPwd", "wrong"));
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldFailLoginWithMissingField() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", "admin"));
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void shouldGetPublicKey() throws Exception {
        mockMvc.perform(get("/auth/public-key").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.keyId").isNotEmpty());
    }

    @Test
    void shouldRefreshToken() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("username", "admin", "cipherPwd", "123456"));
        String loginResp = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginResp, "$.data.accessToken");
        String refreshToken = JsonPath.read(loginResp, "$.data.refreshToken");

        String refreshBody = objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken));
        mockMvc.perform(post("/auth/refresh")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON).content(refreshBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    void shouldLogout() throws Exception {
        String token = loginAsAdmin();
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void shouldGetCurrentUser() throws Exception {
        String token = loginAsAdmin();
        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("admin"));
    }
}
