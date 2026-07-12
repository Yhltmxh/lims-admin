package com.shou.lims.organize.log.service;

import com.shou.lims.common.response.CursorPageVO;
import com.shou.lims.organize.log.dto.LogQueryDTO;
import com.shou.lims.organize.log.vo.LogVO;

public interface LogService {
    CursorPageVO<LogVO> page(LogQueryDTO query);
}
