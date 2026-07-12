package com.shou.lims.organize.log.service;

import com.shou.lims.organize.log.entity.Log;
import com.shou.lims.organize.log.mapper.LogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogSaveService {

    private final LogMapper logMapper;

    @Async
    public void saveLog(Log logEntity) {
        logMapper.insert(logEntity);
    }
}
