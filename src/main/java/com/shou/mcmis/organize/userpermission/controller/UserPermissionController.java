package com.shou.mcmis.organize.userpermission.controller;

import com.shou.mcmis.common.response.Result;
import com.shou.mcmis.organize.log.annotation.Log;
import com.shou.mcmis.organize.userpermission.dto.ForceLogoutDTO;
import com.shou.mcmis.organize.userpermission.dto.UserPermissionCreateDTO;
import com.shou.mcmis.organize.userpermission.dto.UserPermissionRevokeDTO;
import com.shou.mcmis.organize.userpermission.dto.UserPermissionUpdateDTO;
import com.shou.mcmis.organize.userpermission.service.UserPermissionService;
import com.shou.mcmis.organize.userpermission.vo.EffectivePermissionVO;
import com.shou.mcmis.organize.userpermission.vo.UserPermissionAuditVO;
import com.shou.mcmis.organize.userpermission.vo.UserPermissionVO;
import com.shou.mcmis.organize.userpermission.vo.PermissionOptionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/system/users")
@RequiredArgsConstructor
@Tag(name = "用户直接权限")
public class UserPermissionController {
    private final UserPermissionService userPermissionService;

    @GetMapping("/permission-options")
    @Operation(summary = "查询直接授权可选权限", operationId = "listUserPermissionOptions")
    @PreAuthorize("hasAnyAuthority('organize:user-permission:list','organize:user-permission:add','organize:user-permission:edit')")
    public Result<List<PermissionOptionVO>> options() {
        return Result.success(userPermissionService.permissionOptions());
    }

    @GetMapping("/{userId}/permission-grants")
    @Operation(summary = "查询用户直接授权", operationId = "listUserPermissionGrants")
    @PreAuthorize("hasAuthority('organize:user-permission:list')")
    public Result<List<UserPermissionVO>> list(@PathVariable @Positive Long userId) {
        return Result.success(userPermissionService.list(userId));
    }

    @GetMapping("/{userId}/permissions/effective")
    @Operation(summary = "查询用户最终有效权限", operationId = "getUserEffectivePermissions")
    @PreAuthorize("hasAuthority('organize:user-permission:list')")
    public Result<EffectivePermissionVO> effective(@PathVariable @Positive Long userId) {
        return Result.success(userPermissionService.effective(userId));
    }

    @PostMapping("/{userId}/permission-grants")
    @Operation(summary = "新增用户直接授权", operationId = "createUserPermissionGrant")
    @PreAuthorize("hasAuthority('organize:user-permission:add')")
    @Log(module = "用户直接权限", action = "新增授权")
    public Result<Long> create(@PathVariable @Positive Long userId,
                               @Valid @RequestBody UserPermissionCreateDTO dto) {
        return Result.success(userPermissionService.create(userId, dto));
    }

    @PutMapping("/{userId}/permission-grants/{grantId}")
    @Operation(summary = "编辑用户直接授权", operationId = "updateUserPermissionGrant")
    @PreAuthorize("hasAuthority('organize:user-permission:edit')")
    @Log(module = "用户直接权限", action = "编辑授权")
    public Result<Void> update(@PathVariable @Positive Long userId,
                               @PathVariable @Positive Long grantId,
                               @Valid @RequestBody UserPermissionUpdateDTO dto) {
        userPermissionService.update(userId, grantId, dto);
        return Result.success();
    }

    @DeleteMapping("/{userId}/permission-grants/{grantId}")
    @Operation(summary = "撤销用户直接授权", operationId = "revokeUserPermissionGrant")
    @PreAuthorize("hasAuthority('organize:user-permission:del')")
    @Log(module = "用户直接权限", action = "撤销授权")
    public Result<Void> revoke(@PathVariable @Positive Long userId,
                               @PathVariable @Positive Long grantId,
                               @Valid @RequestBody UserPermissionRevokeDTO dto) {
        userPermissionService.revoke(userId, grantId, dto.getReason());
        return Result.success();
    }

    @GetMapping("/{userId}/permission-grants/audit")
    @Operation(summary = "查询用户权限审计", operationId = "listUserPermissionAudits")
    @PreAuthorize("hasAuthority('organize:user-permission:audit')")
    public Result<List<UserPermissionAuditVO>> audits(@PathVariable @Positive Long userId) {
        return Result.success(userPermissionService.audits(userId));
    }

    @PostMapping("/{userId}/force-logout")
    @Operation(summary = "强制用户全部会话下线", operationId = "forceLogoutUser")
    @PreAuthorize("hasAuthority('organize:user:force-logout')")
    @Log(module = "用户管理", action = "强制下线")
    public Result<Void> forceLogout(@PathVariable @Positive Long userId,
                                    @Valid @RequestBody ForceLogoutDTO dto) {
        userPermissionService.forceLogout(userId, dto.getReason());
        return Result.success();
    }
}
