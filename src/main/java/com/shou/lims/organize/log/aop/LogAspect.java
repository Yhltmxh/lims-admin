package com.shou.lims.organize.log.aop;

import com.shou.lims.common.util.SecurityUtils;
import com.shou.lims.organize.log.annotation.Log;
import com.shou.lims.organize.log.mapper.LogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

    private final LogMapper logMapper;

    @Around("@annotation(log)")
    public Object around(ProceedingJoinPoint joinPoint, Log log) throws Throwable {
        long start = System.currentTimeMillis();
        com.shou.lims.organize.log.entity.Log logEntity = new com.shou.lims.organize.log.entity.Log();
        logEntity.setModule(log.module());
        logEntity.setAction(log.action());
        logEntity.setUsername(SecurityUtils.getCurrentUsername());
        logEntity.setUserId(SecurityUtils.getCurrentUserId());

        try {
            HttpServletRequest request = ((ServletRequestAttributes)
                    RequestContextHolder.getRequestAttributes()).getRequest();
            logEntity.setMethod(request.getMethod() + " " + request.getRequestURI());
            logEntity.setIp(request.getRemoteAddr());
        } catch (Exception ignored) {
        }

        try {
            Object result = joinPoint.proceed();
            logEntity.setStatus(1);
            logEntity.setDuration((int) (System.currentTimeMillis() - start));
            return result;
        } catch (Throwable t) {
            logEntity.setStatus(0);
            logEntity.setDuration((int) (System.currentTimeMillis() - start));
            logEntity.setResult(t.getMessage());
            throw t;
        } finally {
            saveLog(logEntity);
        }
    }

    @Async
    protected void saveLog(com.shou.lims.organize.log.entity.Log logEntity) {
        logMapper.insert(logEntity);
    }
}
