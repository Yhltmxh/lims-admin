package com.shou.lims.organize.menu.controller;

import com.shou.lims.BaseAuthenticatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MenuControllerTest extends BaseAuthenticatedTest {

    @Test
    void shouldGetTree() throws Exception {
        mockMvc.perform(get("/system/menus/tree")
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void shouldListPermissionOptionsForMenuBinding() throws Exception {
        mockMvc.perform(get("/system/menus/permission-options")
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].code").isNotEmpty());
    }
}
