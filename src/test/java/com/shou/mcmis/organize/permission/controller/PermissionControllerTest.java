package com.shou.mcmis.organize.permission.controller;

import com.shou.mcmis.BaseAuthenticatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PermissionControllerTest extends BaseAuthenticatedTest {

    @Test
    void shouldListPermissions() throws Exception {
        mockMvc.perform(get("/system/permissions?pageNum=1&pageSize=20")
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
