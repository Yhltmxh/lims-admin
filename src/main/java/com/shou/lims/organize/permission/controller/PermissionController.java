package com.shou.lims.organize.permission.controller;

import com.shou.lims.common.response.PageVO;
import com.shou.lims.common.response.Result;
import com.shou.lims.organize.permission.dto.PermissionCreateDTO;
import com.shou.lims.organize.permission.dto.PermissionQueryDTO;
import com.shou.lims.organize.permission.dto.PermissionUpdateDTO;
import com.shou.lims.organize.permission.service.PermissionService;
import com.shou.lims.organize.permission.vo.PermissionVO;
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
@RequestMapping("/system/permissions")
@RequiredArgsConstructor
@Tag(name = "权限管理")
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    @Operation(summary = "分页查询权限", operationId = "listPermissions")
    @PreAuthorize("hasAuthority('organize:permission:list')")
    public Result<PageVO<PermissionVO>> list(@Valid @ParameterObject PermissionQueryDTO query) {
        return Result.success(permissionService.page(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取权限详情", operationId = "getPermissionById")
    @PreAuthorize("hasAuthority('organize:permission:list')")
    public Result<PermissionVO> getById(@PathVariable @Positive Long id) {
        return Result.success(permissionService.getById(id));
    }

    @PostMapping
    @Operation(summary = "新增权限", operationId = "createPermission")
    @PreAuthorize("hasAuthority('organize:permission:add')")
    @Log(module = "权限管理", action = "新增权限")
    public Result<Long> create(@Valid @RequestBody PermissionCreateDTO dto) {
        return Result.success(permissionService.create(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑权限", operationId = "updatePermission")
    @PreAuthorize("hasAuthority('organize:permission:edit')")
    @Log(module = "权限管理", action = "编辑权限")
    public Result<Void> update(@PathVariable @Positive Long id, @Valid @RequestBody PermissionUpdateDTO dto) {
        permissionService.update(id, dto);
        return Result.success();
    }

    @DeleteMapping
    @Operation(summary = "批量删除权限", operationId = "deletePermissions")
    @PreAuthorize("hasAuthority('organize:permission:del')")
    @Log(module = "权限管理", action = "删除权限")
    public Result<Void> delete(@Valid @RequestBody @NotEmpty @Size(max = 500)
                               List<@Positive Long> ids) {
        permissionService.delete(ids);
        return Result.success();
    }
}
