package com.shou.lims.organize.log.aop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "cipherpwd", "refreshtoken", "accesstoken", "token", "secret");

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
            JsonNode args = objectMapper.valueToTree(joinPoint.getArgs());
            redact(args);
            logEntity.setParams(objectMapper.writeValueAsString(args));
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

    private void redact(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (SENSITIVE_FIELDS.contains(field.getKey().toLowerCase())) {
                    objectNode.put(field.getKey(), "***");
                } else {
                    redact(field.getValue());
                }
            }
        } else if (node instanceof ArrayNode arrayNode) {
            arrayNode.forEach(this::redact);
        }
    }
}
