package com.shou.lims.organize.log.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shou.lims.common.response.CursorPageVO;
import com.shou.lims.organize.log.converter.LogConverter;
import com.shou.lims.organize.log.dto.LogQueryDTO;
import com.shou.lims.organize.log.entity.Log;
import com.shou.lims.organize.log.mapper.LogMapper;
import com.shou.lims.organize.log.service.LogService;
import com.shou.lims.organize.log.vo.LogVO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

    private final LogMapper logMapper;
    private final LogConverter logConverter;

    @Override
    public CursorPageVO<LogVO> page(LogQueryDTO query) {
        int pageSize = query.getPageSize() != null ? query.getPageSize() : 10;
        Long cursor = query.getLastId() != null ? query.getLastId() : 0L;

        LambdaQueryWrapper<Log> wrapper = new LambdaQueryWrapper<Log>()
                .eq(Log::getIsDelete, 0)
                .like(StringUtils.isNotBlank(query.getUsername()), Log::getUsername, query.getUsername())
                .eq(StringUtils.isNotBlank(query.getModule()), Log::getModule, query.getModule())
                .eq(StringUtils.isNotBlank(query.getAction()), Log::getAction, query.getAction())
                .ge(query.getStartTime() != null, Log::getCreateTime, query.getStartTime())
                .le(query.getEndTime() != null, Log::getCreateTime, query.getEndTime())
                .lt(cursor > 0, Log::getId, cursor)
                .orderByDesc(Log::getId)
                .last("LIMIT " + (pageSize + 1));

        List<Log> list = logMapper.selectList(wrapper);
        boolean hasMore = list.size() > pageSize;
        if (hasMore) {
            list = list.subList(0, pageSize);
        }
        Long nextCursor = hasMore && !list.isEmpty() ? list.get(list.size() - 1).getId() : null;
        List<LogVO> voList = logConverter.toVOList(list);
        return new CursorPageVO<>(voList, nextCursor, hasMore);
    }
}
