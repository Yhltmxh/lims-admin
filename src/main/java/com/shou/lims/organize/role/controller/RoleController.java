package com.shou.lims.organize.role.controller;

import com.shou.lims.common.response.PageVO;
import com.shou.lims.common.response.Result;
import com.shou.lims.organize.role.dto.RoleCreateDTO;
import com.shou.lims.organize.role.dto.RoleQueryDTO;
import com.shou.lims.organize.role.dto.RoleUpdateDTO;
import com.shou.lims.organize.role.service.RoleService;
import com.shou.lims.organize.role.vo.RoleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/system/roles")
@RequiredArgsConstructor
@Tag(name = "角色管理")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @Operation(summary = "分页查询角色")
    @PreAuthorize("hasAuthority('organize:role')")
    public Result<PageVO<RoleVO>> list(@Valid RoleQueryDTO query) {
        return Result.success(roleService.page(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取角色详情")
    @PreAuthorize("hasAuthority('organize:role')")
    public Result<RoleVO> getById(@PathVariable Long id) {
        return Result.success(roleService.getById(id));
    }

    @PostMapping
    @Operation(summary = "新增角色")
    @PreAuthorize("hasAuthority('organize:role:add')")
    public Result<Long> create(@Valid @RequestBody RoleCreateDTO dto) {
        return Result.success(roleService.create(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑角色")
    @PreAuthorize("hasAuthority('organize:role:edit')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody RoleUpdateDTO dto) {
        roleService.update(id, dto);
        return Result.success();
    }

    @DeleteMapping
    @Operation(summary = "批量删除角色")
    @PreAuthorize("hasAuthority('organize:role:del')")
    public Result<Void> delete(@RequestBody @NotEmpty List<Long> ids) {
        roleService.delete(ids);
        return Result.success();
    }

    @PostMapping("/{id}/permissions")
    @Operation(summary = "分配权限")
    @PreAuthorize("hasAuthority('organize:role')")
    public Result<Void> assignPermissions(@PathVariable Long id, @RequestBody @NotEmpty List<Long> permissionIds) {
        roleService.assignPermissions(id, permissionIds);
        return Result.success();
    }

    @PostMapping("/{id}/menus")
    @Operation(summary = "分配菜单")
    @PreAuthorize("hasAuthority('organize:role')")
    public Result<Void> assignMenus(@PathVariable Long id, @RequestBody @NotEmpty List<Long> menuIds) {
        roleService.assignMenus(id, menuIds);
        return Result.success();
    }
}
