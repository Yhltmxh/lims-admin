package com.shou.lims.organize.log.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shou.lims.organize.log.annotation.Log;
import com.shou.lims.organize.log.service.LogSaveService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogAspectTest {

    @Mock
    private LogSaveService logSaveService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private LogAspect logAspect;

    @BeforeEach
    void setUp() {
        logAspect = new LogAspect(logSaveService, objectMapper);
    }

    private Log logAnnotation(String module, String action) {
        Log log = mock(Log.class);
        when(log.module()).thenReturn(module);
        when(log.action()).thenReturn(action);
        return log;
    }

    @Test
    void shouldRecordSuccessLog() throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.getArgs()).thenReturn(new Object[]{"arg1"});
        when(pjp.proceed()).thenReturn("ok");

        Object result = logAspect.around(pjp, logAnnotation("测试模块", "新增"));

        assertThat(result).isEqualTo("ok");
        ArgumentCaptor<com.shou.lims.organize.log.entity.Log> captor =
                ArgumentCaptor.forClass(com.shou.lims.organize.log.entity.Log.class);
        verify(logSaveService).saveLog(captor.capture());
        com.shou.lims.organize.log.entity.Log saved = captor.getValue();
        assertThat(saved.getModule()).isEqualTo("测试模块");
        assertThat(saved.getAction()).isEqualTo("新增");
        assertThat(saved.getStatus()).isEqualTo(1);
        assertThat(saved.getParams()).contains("arg1");
        assertThat(saved.getDuration()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void shouldRecordFailureLogAndRethrow() throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.getArgs()).thenReturn(new Object[]{});
        when(pjp.proceed()).thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> logAspect.around(pjp, logAnnotation("M", "A")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("boom");

        ArgumentCaptor<com.shou.lims.organize.log.entity.Log> captor =
                ArgumentCaptor.forClass(com.shou.lims.organize.log.entity.Log.class);
        verify(logSaveService).saveLog(captor.capture());
        com.shou.lims.organize.log.entity.Log saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(0);
        assertThat(saved.getResult()).contains("boom");
    }

    @Test
    void shouldHandleSerializationFailureGracefully() throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        // An object whose getter throws makes Jackson serialization fail
        when(pjp.getArgs()).thenReturn(new Object[]{new Unserializable()});
        when(pjp.proceed()).thenReturn("ok");

        Object result = logAspect.around(pjp, logAnnotation("M", "A"));

        assertThat(result).isEqualTo("ok"); // business method not interrupted
        ArgumentCaptor<com.shou.lims.organize.log.entity.Log> captor =
                ArgumentCaptor.forClass(com.shou.lims.organize.log.entity.Log.class);
        verify(logSaveService).saveLog(captor.capture());
        assertThat(captor.getValue().getParams()).contains("serialization failed");
    }

    static class Unserializable {
        public String getValue() {
            throw new RuntimeException("cannot serialize this");
        }
    }
}
