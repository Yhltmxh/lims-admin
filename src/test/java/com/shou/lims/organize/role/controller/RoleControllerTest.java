package com.shou.lims.organize.role.controller;

import com.shou.lims.BaseAuthenticatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RoleControllerTest extends BaseAuthenticatedTest {

    @Test
    void shouldListRoles() throws Exception {
        mockMvc.perform(get("/system/roles?pageNum=1&pageSize=10")
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void shouldCreateRole() throws Exception {
        String json = "{\"name\":\"ROLE_TEST_CTL\",\"label\":\"控制器测试\",\"status\":1}";
        mockMvc.perform(post("/system/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void shouldAssignPermissions() throws Exception {
        String json = "[1,2]";
        mockMvc.perform(post("/system/roles/2/permissions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void shouldAssignMenus() throws Exception {
        String json = "[1,2]";
        mockMvc.perform(post("/system/roles/2/menus")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
