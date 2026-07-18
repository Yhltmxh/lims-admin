package com.shou.lims.organize.dept.service.impl;

import com.shou.lims.BaseSpringBootTest;
import com.shou.lims.common.exception.BusinessException;
import com.shou.lims.common.exception.NotFoundException;
import com.shou.lims.organize.dept.dto.DeptCreateDTO;
import com.shou.lims.organize.dept.service.DeptService;
import com.shou.lims.organize.dept.vo.DeptVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class DeptServiceImplTest extends BaseSpringBootTest {

    @Autowired
    private DeptService deptService;

    @Test
    void shouldGetTree() {
        List<DeptVO> tree = deptService.getTree();
        assertThat(tree).isNotEmpty();
        assertThat(tree).allMatch(dept -> dept.getName() != null && !dept.getName().isBlank());
    }

    @Test
    void shouldGetById() {
        assertThat(deptService.getById(1L).getName()).isNotBlank();
    }

    @Test
    void shouldThrowNotFoundForMissingDept() {
        assertThatThrownBy(() -> deptService.getById(9999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldCreateDept() {
        DeptCreateDTO dto = new DeptCreateDTO();
        dto.setName("新部门");
        dto.setParentId(1L);
        Long id = deptService.create(dto);
        assertThat(id).isNotNull();
        assertThat(deptService.getById(id).getName()).isEqualTo("新部门");
    }

    @Test
    void shouldRejectDuplicateDeptName() {
        DeptCreateDTO dto = new DeptCreateDTO();
        dto.setName(deptService.getById(1L).getName());
        assertThatThrownBy(() -> deptService.create(dto))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(409);
    }

    @Test
    void shouldDeleteDept() {
        DeptCreateDTO dto = new DeptCreateDTO();
        dto.setName("待删除部门");
        Long id = deptService.create(dto);
        deptService.delete(List.of(id));
        assertThatThrownBy(() -> deptService.getById(id))
                .isInstanceOf(NotFoundException.class);
    }
}
