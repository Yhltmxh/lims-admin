package com.shou.lims.organize.user.controller;

import com.shou.lims.BaseAuthenticatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerTest extends BaseAuthenticatedTest {

    @Test
    void shouldListUsers() throws Exception {
        mockMvc.perform(get("/system/users?pageNum=1&pageSize=10")
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    void shouldGetUserById() throws Exception {
        mockMvc.perform(get("/system/users/1")
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    @Test
    void shouldReturn404ForMissingUser() throws Exception {
        mockMvc.perform(get("/system/users/9999")
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void shouldCreateUser() throws Exception {
        String json = "{\"username\":\"newuser\",\"password\":\"123456\",\"realName\":\"新用户\",\"roleIds\":[1]}";
        mockMvc.perform(post("/system/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNumber());
    }

    @Test
    void shouldRejectCreateWithMissingField() throws Exception {
        String json = "{\"username\":\"nopwd\"}";
        mockMvc.perform(post("/system/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void shouldRejectDuplicateUsername() throws Exception {
        String json = "{\"username\":\"admin\",\"password\":\"123456\",\"realName\":\"dup\",\"roleIds\":[1]}";
        mockMvc.perform(post("/system/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void shouldUpdateUser() throws Exception {
        String json = "{\"realName\":\"updated\"}";
        mockMvc.perform(put("/system/users/2")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void shouldDeleteUser() throws Exception {
        String json = "[3]";
        mockMvc.perform(delete("/system/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void shouldRejectWithoutToken() throws Exception {
        mockMvc.perform(get("/system/users").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
