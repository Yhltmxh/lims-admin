package com.shou.lims;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class BaseAuthenticatedTest extends BaseSpringBootTest {
    protected String adminToken;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void loginAsAdmin() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("username", "admin", "cipherPwd", "123456"));
        String response = mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        adminToken = JsonPath.read(response, "$.data.accessToken");
    }
}
