package com.shou.mcmis.organize.log.service.impl;

import com.shou.mcmis.BaseSpringBootTest;
import com.shou.mcmis.common.response.CursorPageVO;
import com.shou.mcmis.organize.log.dto.LogQueryDTO;
import com.shou.mcmis.organize.log.entity.Log;
import com.shou.mcmis.organize.log.mapper.LogMapper;
import com.shou.mcmis.organize.log.service.LogService;
import com.shou.mcmis.organize.log.vo.LogVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class LogServiceImplTest extends BaseSpringBootTest {

    @Autowired
    private LogService logService;
    @Autowired
    private LogMapper logMapper;

    @BeforeEach
    void seedLogs() {
        for (int i = 1; i <= 5; i++) {
            Log log = new Log();
            log.setUserId(1L);
            log.setUsername("admin");
            log.setModule("测试模块");
            log.setAction("操作" + i);
            log.setMethod("GET /test/" + i);
            log.setStatus(1);
            log.setDuration(10);
            log.setIsDelete(0);
            log.setCreateTime(LocalDateTime.now());
            logMapper.insert(log);
        }
    }

    @Test
    void shouldPageWithCursor() {
        LogQueryDTO query = new LogQueryDTO();
        query.setPageSize(2);

        CursorPageVO<LogVO> result = logService.page(query);

        assertThat(result.getRecords()).hasSize(2);
        assertThat(result.getHasMore()).isTrue();
        assertThat(result.getNextCursor()).isNotNull();
    }

    @Test
    void shouldPageNextPage() {
        LogQueryDTO query1 = new LogQueryDTO();
        query1.setPageSize(2);
        CursorPageVO<LogVO> page1 = logService.page(query1);

        LogQueryDTO query2 = new LogQueryDTO();
        query2.setPageSize(2);
        query2.setLastId(page1.getNextCursor());
        CursorPageVO<LogVO> page2 = logService.page(query2);

        // 游标降序分页：page2 的记录 id 应严格小于 page1 的最小 id，且两页无重叠。
        assertThat(page2.getRecords()).isNotEmpty();
        long page1MinId = page1.getRecords().stream().mapToLong(LogVO::getId).min().orElseThrow();
        assertThat(page2.getRecords()).allSatisfy(vo ->
                assertThat(vo.getId()).isLessThan(page1MinId));
    }

    @Test
    void shouldReturnHasMoreFalseAtEnd() {
        LogQueryDTO query = new LogQueryDTO();
        query.setPageSize(1000);

        CursorPageVO<LogVO> result = logService.page(query);

        assertThat(result.getHasMore()).isFalse();
        assertThat(result.getNextCursor()).isNull();
    }
}
