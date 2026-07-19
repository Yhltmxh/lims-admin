package com.shou.mcmis.organize.log.service;

import com.shou.mcmis.organize.log.entity.Log;
import com.shou.mcmis.organize.log.mapper.LogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogSaveService {

    private final LogMapper logMapper;

    @Async
    public void saveLog(Log logEntity) {
        try {
            logMapper.insert(logEntity);
        } catch (Exception e) {
            log.error("操作日志写入失败: module={}, action={}", logEntity.getModule(), logEntity.getAction(), e);
        }
    }
}
