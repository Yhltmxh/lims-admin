package com.shou.mcmis.organize.log.converter;

import com.shou.mcmis.organize.log.entity.Log;
import com.shou.mcmis.organize.log.vo.LogVO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LogConverter {
    LogVO toVO(Log entity);
    List<LogVO> toVOList(List<Log> entityList);
}
