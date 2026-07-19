package com.shou.mcmis.organize.log.service;

import com.shou.mcmis.common.response.CursorPageVO;
import com.shou.mcmis.organize.log.dto.LogQueryDTO;
import com.shou.mcmis.organize.log.vo.LogVO;

public interface LogService {
    CursorPageVO<LogVO> page(LogQueryDTO query);
}
