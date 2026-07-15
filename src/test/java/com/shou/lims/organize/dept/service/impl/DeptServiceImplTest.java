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

    /**
     * 种子数据结构说明：init.sql 的部门插入语句
     * {@code INSERT INTO sys_dept (name, sort_order, leader)} 未指定 parent_id，
     * 列默认值为 0，因此 4 个部门（总公司/检测部/质控部/综合部）全部是根节点，
     * 树中不存在嵌套 children。这里只断言根节点列表本身，不断言 children 非空。
     */
    @Test
    void shouldGetTree() {
        List<DeptVO> tree = deptService.getTree();
        assertThat(tree).isNotEmpty();
        assertThat(tree).extracting(DeptVO::getName).contains("总公司", "检测部", "质控部", "综合部");
    }

    @Test
    void shouldGetById() {
        assertThat(deptService.getById(1L).getName()).isEqualTo("总公司");
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
        dto.setName("总公司");
        assertThatThrownBy(() -> deptService.create(dto))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(409);
    }

    @Test
    void shouldDeleteDept() {
        deptService.delete(List.of(3L));
        assertThatThrownBy(() -> deptService.getById(3L))
                .isInstanceOf(NotFoundException.class);
    }
}
