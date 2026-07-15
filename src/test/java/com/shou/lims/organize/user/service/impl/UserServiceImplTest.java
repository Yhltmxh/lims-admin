package com.shou.lims.organize.user.service.impl;

import com.shou.lims.BaseSpringBootTest;
import com.shou.lims.common.exception.BusinessException;
import com.shou.lims.common.exception.NotFoundException;
import com.shou.lims.common.response.PageVO;
import com.shou.lims.organize.user.dto.UserCreateDTO;
import com.shou.lims.organize.user.dto.UserQueryDTO;
import com.shou.lims.organize.user.dto.UserUpdateDTO;
import com.shou.lims.organize.user.service.UserService;
import com.shou.lims.organize.user.vo.UserVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class UserServiceImplTest extends BaseSpringBootTest {

    @Autowired
    private UserService userService;

    @Test
    void shouldPageUsers() {
        UserQueryDTO query = new UserQueryDTO();
        query.setPageNum(1);
        query.setPageSize(2);

        PageVO<UserVO> result = userService.page(query);

        assertThat(result.getRecords()).hasSize(2);
        assertThat(result.getTotal()).isGreaterThanOrEqualTo(4);
    }

    @Test
    void shouldFilterByUsername() {
        UserQueryDTO query = new UserQueryDTO();
        query.setPageNum(1);
        query.setPageSize(10);
        query.setUsername("admin");

        PageVO<UserVO> result = userService.page(query);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords().get(0).getUsername()).isEqualTo("admin");
    }

    @Test
    void shouldGetById() {
        UserVO user = userService.getById(1L);
        assertThat(user.getUsername()).isEqualTo("admin");
    }

    @Test
    void shouldThrowNotFoundForMissingUser() {
        assertThatThrownBy(() -> userService.getById(9999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldCreateUser() {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setUsername("testuser");
        dto.setPassword("123456");
        dto.setRealName("测试用户");

        Long id = userService.create(dto);

        assertThat(id).isNotNull();
        UserVO created = userService.getById(id);
        assertThat(created.getUsername()).isEqualTo("testuser");
    }

    @Test
    void shouldRejectDuplicateUsername() {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setUsername("admin");
        dto.setPassword("123456");

        assertThatThrownBy(() -> userService.create(dto))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(409);
    }

    @Test
    void shouldUpdateUser() {
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setRealName("新名字");
        userService.update(2L, dto);

        UserVO updated = userService.getById(2L);
        assertThat(updated.getRealName()).isEqualTo("新名字");
    }

    @Test
    void shouldThrowNotFoundForUpdateMissingUser() {
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setRealName("x");
        assertThatThrownBy(() -> userService.update(9999L, dto))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldDeleteUser() {
        userService.delete(List.of(2L));
        assertThatThrownBy(() -> userService.getById(2L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldNotThrowOnDeleteEmptyList() {
        assertThatCode(() -> userService.delete(List.of())).doesNotThrowAnyException();
    }
}
