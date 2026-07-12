package com.shou.lims.organize.log.converter;

import com.shou.lims.organize.log.entity.Log;
import com.shou.lims.organize.log.vo.LogVO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LogConverter {
    LogVO toVO(Log entity);
    List<LogVO> toVOList(List<Log> entityList);
}
