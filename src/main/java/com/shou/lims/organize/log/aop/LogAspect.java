package com.shou.lims.organize.log.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shou.lims.common.util.SecurityUtils;
import com.shou.lims.organize.log.annotation.Log;
import com.shou.lims.organize.log.service.LogSaveService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

    private final LogSaveService logSaveService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(log)")
    public Object around(ProceedingJoinPoint joinPoint, Log log) throws Throwable {
        long start = System.currentTimeMillis();
        com.shou.lims.organize.log.entity.Log logEntity = new com.shou.lims.organize.log.entity.Log();
        logEntity.setModule(log.module());
        logEntity.setAction(log.action());
        logEntity.setUsername(SecurityUtils.getCurrentUsername());
        logEntity.setUserId(SecurityUtils.getCurrentUserId());

        try {
            logEntity.setParams(objectMapper.writeValueAsString(joinPoint.getArgs()));
        } catch (Exception e) {
            logEntity.setParams("[serialization failed: " + e.getMessage() + "]");
        }

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
            logSaveService.saveLog(logEntity);
        }
    }
}
