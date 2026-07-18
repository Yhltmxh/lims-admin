package com.shou.lims.organize.dept.controller;

import com.shou.lims.BaseAuthenticatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DeptControllerTest extends BaseAuthenticatedTest {

    @Test
    void shouldGetTree() throws Exception {
        mockMvc.perform(get("/system/depts/tree")
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void shouldGetById() throws Exception {
        mockMvc.perform(get("/system/depts/1")
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").isNotEmpty());
    }

    @Test
    void shouldCreateDept() throws Exception {
        String json = "{\"name\":\"新部门\",\"parentId\":1}";
        mockMvc.perform(post("/system/depts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
