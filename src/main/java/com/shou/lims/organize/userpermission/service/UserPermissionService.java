package com.shou.lims.organize.userpermission.service;

import com.shou.lims.organize.userpermission.dto.UserPermissionCreateDTO;
import com.shou.lims.organize.userpermission.dto.UserPermissionUpdateDTO;
import com.shou.lims.organize.userpermission.vo.EffectivePermissionVO;
import com.shou.lims.organize.userpermission.vo.UserPermissionAuditVO;
import com.shou.lims.organize.userpermission.vo.UserPermissionVO;
import com.shou.lims.organize.userpermission.vo.PermissionOptionVO;

import java.util.List;

public interface UserPermissionService {
    List<UserPermissionVO> list(Long userId);
    EffectivePermissionVO effective(Long userId);
    Long create(Long userId, UserPermissionCreateDTO dto);
    void update(Long userId, Long grantId, UserPermissionUpdateDTO dto);
    void revoke(Long userId, Long grantId, String reason);
    List<UserPermissionAuditVO> audits(Long userId);
    void forceLogout(Long userId, String reason);
    List<PermissionOptionVO> permissionOptions();
}
