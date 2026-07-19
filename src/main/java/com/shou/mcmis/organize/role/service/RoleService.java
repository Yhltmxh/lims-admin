package com.shou.mcmis.organize.role.service;

import com.shou.mcmis.common.response.PageVO;
import com.shou.mcmis.organize.role.dto.RoleCreateDTO;
import com.shou.mcmis.organize.role.dto.RoleQueryDTO;
import com.shou.mcmis.organize.role.dto.RoleUpdateDTO;
import com.shou.mcmis.organize.role.vo.RoleVO;
import java.util.List;

public interface RoleService {
    PageVO<RoleVO> page(RoleQueryDTO query);
    RoleVO getById(Long id);
    Long create(RoleCreateDTO dto);
    void update(Long id, RoleUpdateDTO dto);
    void delete(List<Long> ids);
    void assignPermissions(Long roleId, List<Long> permissionIds);
    void assignMenus(Long roleId, List<Long> menuIds);
}
