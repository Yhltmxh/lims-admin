package com.shou.mcmis.organize.userpermission.controller;

import com.shou.mcmis.BaseAuthenticatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserPermissionControllerTest extends BaseAuthenticatedTest {

    @Test
    void shouldReturnEffectivePermissions() throws Exception {
        mockMvc.perform(get("/system/users/2/permissions/effective")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.superAdmin").value(false))
                .andExpect(jsonPath("$.data.permissions").isArray());
    }

    @Test
    void shouldCreateScheduledAllowGrant() throws Exception {
        String json = """
                {
                  "permissionId": 1,
                  "effect": 1,
                  "validFrom": "2030-01-01T08:00:00",
                  "expiresAt": "2030-01-01T12:00:00",
                  "reason": "测试提前排期授权"
                }
                """;
        mockMvc.perform(post("/system/users/2/permission-grants")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isNumber());
    }

    @Test
    void shouldRejectDenyForSuperAdmin() throws Exception {
        String json = """
                {
                  "permissionId": 1,
                  "effect": 2,
                  "reason": "超级管理员DENY测试"
                }
                """;
        mockMvc.perform(post("/system/users/1/permission-grants")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("超级管理员不能配置DENY权限"));
    }

    @Test
    void shouldForceUserLogoutWithoutDisablingUser() throws Exception {
        mockMvc.perform(post("/system/users/2/force-logout")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"安全测试\"}"))
                .andExpect(status().isOk());
    }
}
