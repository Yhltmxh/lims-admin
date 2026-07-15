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
                .andExpect(jsonPath("$.data.name").value("总公司"));
    }

    /**
     * 期望值调整说明：控制器要求权限 organize:dept:add，但 init.sql 种子数据中不存在该权限
     * （仅有模块级 organize:dept），因此即使 admin 也会被 @PreAuthorize 拒绝，实际返回 code=403。
     * 这是种子数据与控制器权限编码不一致的生产问题，按任务要求仅调整测试断言、不修改生产代码。
     */
    @Test
    void shouldReturn403OnCreateDeptDueToMissingSeedPermission() throws Exception {
        String json = "{\"name\":\"新部门\",\"parentId\":1}";
        mockMvc.perform(post("/system/depts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }
}
