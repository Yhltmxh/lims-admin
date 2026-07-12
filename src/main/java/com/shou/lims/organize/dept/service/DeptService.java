package com.shou.lims.organize.dept.service;

import com.shou.lims.common.response.PageVO;
import com.shou.lims.organize.dept.dto.DeptCreateDTO;
import com.shou.lims.organize.dept.dto.DeptQueryDTO;
import com.shou.lims.organize.dept.dto.DeptUpdateDTO;
import com.shou.lims.organize.dept.vo.DeptVO;
import java.util.List;

public interface DeptService {
    PageVO<DeptVO> page(DeptQueryDTO query);
    DeptVO getById(Long id);
    Long create(DeptCreateDTO dto);
    void update(Long id, DeptUpdateDTO dto);
    void delete(List<Long> ids);
    List<DeptVO> getTree();
}
