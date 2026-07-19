package com.shou.mcmis.organize.log.controller;

import com.shou.mcmis.BaseAuthenticatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LogControllerTest extends BaseAuthenticatedTest {

    @Test
    void shouldListLogs() throws Exception {
        mockMvc.perform(get("/system/logs?pageSize=10")
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }
}
