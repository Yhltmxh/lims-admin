package com.shou.mcmis.organize.permission.service.impl;

import com.shou.mcmis.BaseSpringBootTest;
import com.shou.mcmis.common.response.PageVO;
import com.shou.mcmis.organize.permission.dto.PermissionQueryDTO;
import com.shou.mcmis.organize.permission.service.PermissionService;
import com.shou.mcmis.organize.permission.vo.PermissionVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.*;

class PermissionServiceImplTest extends BaseSpringBootTest {

    @Autowired
    private PermissionService permissionService;

    @Test
    void shouldPagePermissions() {
        PermissionQueryDTO query = new PermissionQueryDTO();
        query.setPageNum(1);
        query.setPageSize(10);
        PageVO<PermissionVO> result = permissionService.page(query);
        assertThat(result.getTotal()).isGreaterThanOrEqualTo(8);
    }

    @Test
    void shouldFilterByCode() {
        PermissionQueryDTO query = new PermissionQueryDTO();
        query.setPageNum(1);
        query.setPageSize(10);
        query.setCode("organize:user:list");
        PageVO<PermissionVO> result = permissionService.page(query);
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords().get(0).getCode()).isEqualTo("organize:user:list");
    }
}
