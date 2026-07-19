package com.shou.mcmis.organize.permission.service;

import com.shou.mcmis.common.response.PageVO;
import com.shou.mcmis.organize.permission.dto.PermissionCreateDTO;
import com.shou.mcmis.organize.permission.dto.PermissionQueryDTO;
import com.shou.mcmis.organize.permission.dto.PermissionUpdateDTO;
import com.shou.mcmis.organize.permission.vo.PermissionVO;
import java.util.List;

public interface PermissionService {
    PageVO<PermissionVO> page(PermissionQueryDTO query);
    PermissionVO getById(Long id);
    Long create(PermissionCreateDTO dto);
    void update(Long id, PermissionUpdateDTO dto);
    void delete(List<Long> ids);
}
