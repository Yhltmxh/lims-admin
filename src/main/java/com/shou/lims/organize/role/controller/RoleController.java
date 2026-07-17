package com.shou.lims.organize.role.controller;

import com.shou.lims.common.response.PageVO;
import com.shou.lims.common.response.Result;
import com.shou.lims.organize.role.dto.RoleCreateDTO;
import com.shou.lims.organize.role.dto.RoleQueryDTO;
import com.shou.lims.organize.role.dto.RoleUpdateDTO;
import com.shou.lims.organize.role.service.RoleService;
import com.shou.lims.organize.role.vo.RoleVO;
import com.shou.lims.organize.log.annotation.Log;
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
@RequestMapping("/system/roles")
@RequiredArgsConstructor
@Tag(name = "角色管理")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @Operation(summary = "分页查询角色", operationId = "listRoles")
    @PreAuthorize("hasAuthority('organize:role:list')")
    public Result<PageVO<RoleVO>> list(@Valid @ParameterObject RoleQueryDTO query) {
        return Result.success(roleService.page(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取角色详情", operationId = "getRoleById")
    @PreAuthorize("hasAuthority('organize:role:list')")
    public Result<RoleVO> getById(@PathVariable @Positive Long id) {
        return Result.success(roleService.getById(id));
    }

    @PostMapping
    @Operation(summary = "新增角色", operationId = "createRole")
    @PreAuthorize("hasAuthority('organize:role:add')")
    @Log(module = "角色管理", action = "新增角色")
    public Result<Long> create(@Valid @RequestBody RoleCreateDTO dto) {
        return Result.success(roleService.create(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑角色", operationId = "updateRole")
    @PreAuthorize("hasAuthority('organize:role:edit')")
    @Log(module = "角色管理", action = "编辑角色")
    public Result<Void> update(@PathVariable @Positive Long id, @Valid @RequestBody RoleUpdateDTO dto) {
        roleService.update(id, dto);
        return Result.success();
    }

    @DeleteMapping
    @Operation(summary = "批量删除角色", operationId = "deleteRoles")
    @PreAuthorize("hasAuthority('organize:role:del')")
    @Log(module = "角色管理", action = "删除角色")
    public Result<Void> delete(@Valid @RequestBody @NotEmpty @Size(max = 500)
                               List<@Positive Long> ids) {
        roleService.delete(ids);
        return Result.success();
    }

    @PostMapping("/{id}/permissions")
    @Operation(summary = "分配权限", operationId = "assignRolePermissions")
    @PreAuthorize("hasAuthority('organize:role:edit')")
    @Log(module = "角色管理", action = "分配权限")
    public Result<Void> assignPermissions(@PathVariable @Positive Long id,
                                          @Valid @RequestBody @Size(max = 500)
                                          List<@Positive Long> permissionIds) {
        roleService.assignPermissions(id, permissionIds);
        return Result.success();
    }

    @PostMapping("/{id}/menus")
    @Operation(summary = "分配菜单", operationId = "assignRoleMenus")
    @PreAuthorize("hasAuthority('organize:role:edit')")
    @Log(module = "角色管理", action = "分配菜单")
    public Result<Void> assignMenus(@PathVariable @Positive Long id,
                                    @Valid @RequestBody @Size(max = 500)
                                    List<@Positive Long> menuIds) {
        roleService.assignMenus(id, menuIds);
        return Result.success();
    }
}
