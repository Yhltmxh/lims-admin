package com.shou.lims.organize.permission.service;

import com.shou.lims.common.response.PageVO;
import com.shou.lims.organize.permission.dto.PermissionCreateDTO;
import com.shou.lims.organize.permission.dto.PermissionQueryDTO;
import com.shou.lims.organize.permission.dto.PermissionUpdateDTO;
import com.shou.lims.organize.permission.vo.PermissionVO;
import java.util.List;

public interface PermissionService {
    PageVO<PermissionVO> page(PermissionQueryDTO query);
    PermissionVO getById(Long id);
    Long create(PermissionCreateDTO dto);
    void update(Long id, PermissionUpdateDTO dto);
    void delete(List<Long> ids);
}
