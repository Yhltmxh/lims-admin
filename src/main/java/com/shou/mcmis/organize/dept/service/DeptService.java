package com.shou.mcmis.organize.dept.service;

import com.shou.mcmis.common.response.PageVO;
import com.shou.mcmis.organize.dept.dto.DeptCreateDTO;
import com.shou.mcmis.organize.dept.dto.DeptQueryDTO;
import com.shou.mcmis.organize.dept.dto.DeptUpdateDTO;
import com.shou.mcmis.organize.dept.vo.DeptVO;
import java.util.List;

public interface DeptService {
    PageVO<DeptVO> page(DeptQueryDTO query);
    DeptVO getById(Long id);
    Long create(DeptCreateDTO dto);
    void update(Long id, DeptUpdateDTO dto);
    void delete(List<Long> ids);
    List<DeptVO> getTree();
}
