package com.shou.mcmis.organize.dept.converter;

import com.shou.mcmis.organize.dept.dto.DeptCreateDTO;
import com.shou.mcmis.organize.dept.entity.Dept;
import com.shou.mcmis.organize.dept.vo.DeptVO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DeptConverter {
    DeptVO toVO(Dept entity);
    Dept toEntity(DeptCreateDTO dto);
    List<DeptVO> toVOList(List<Dept> entityList);
}
