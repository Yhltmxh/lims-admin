package com.shou.mcmis.organize.user.controller;

import com.shou.mcmis.common.response.PageVO;
import com.shou.mcmis.common.response.Result;
import com.shou.mcmis.organize.user.dto.UserCreateDTO;
import com.shou.mcmis.organize.user.dto.UserQueryDTO;
import com.shou.mcmis.organize.user.dto.UserUpdateDTO;
import com.shou.mcmis.organize.user.service.UserService;
import com.shou.mcmis.organize.user.vo.UserVO;
import com.shou.mcmis.organize.log.annotation.Log;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/system/users")
@RequiredArgsConstructor
@Tag(name = "用户管理")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "分页查询用户", operationId = "listUsers")
    @PreAuthorize("hasAuthority('organize:user:list')")
    public Result<PageVO<UserVO>> list(@Valid @ParameterObject UserQueryDTO query) {
        return Result.success(userService.page(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取用户详情", operationId = "getUserById")
    @PreAuthorize("hasAuthority('organize:user:list')")
    public Result<UserVO> getById(@PathVariable @Positive Long id) {
        return Result.success(userService.getById(id));
    }

    @PostMapping
    @Operation(summary = "新增用户", operationId = "createUser")
    @PreAuthorize("hasAuthority('organize:user:add')")
    @Log(module = "用户管理", action = "新增用户")
    public Result<Long> create(@Valid @RequestBody UserCreateDTO dto) {
        return Result.success(userService.create(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑用户", operationId = "updateUser")
    @PreAuthorize("hasAuthority('organize:user:edit')")
    @Log(module = "用户管理", action = "编辑用户")
    public Result<Void> update(@PathVariable @Positive Long id, @Valid @RequestBody UserUpdateDTO dto) {
        userService.update(id, dto);
        return Result.success();
    }

    @DeleteMapping
    @Operation(summary = "批量删除用户", operationId = "deleteUsers")
    @PreAuthorize("hasAuthority('organize:user:del')")
    @Log(module = "用户管理", action = "删除用户")
    public Result<Void> delete(@Valid @RequestBody @NotEmpty @Size(max = 500)
                               List<@Positive Long> ids) {
        userService.delete(ids);
        return Result.success();
    }
}
