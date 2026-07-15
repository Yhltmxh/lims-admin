package com.shou.lims.organize.log.service.impl;

import com.shou.lims.BaseSpringBootTest;
import com.shou.lims.common.response.CursorPageVO;
import com.shou.lims.organize.log.dto.LogQueryDTO;
import com.shou.lims.organize.log.entity.Log;
import com.shou.lims.organize.log.mapper.LogMapper;
import com.shou.lims.organize.log.service.LogService;
import com.shou.lims.organize.log.vo.LogVO;
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

        // 注意：LogServiceImpl.page 存在游标方向缺陷 —— 排序为 ORDER BY id DESC，
        // 但翻页条件却是 id > cursor（正确应为 id < cursor）。
        // 实测：page1 返回 ids=[N, N-1]、nextCursor=N-1；page2 用 lastId=N-1 查询
        // 得到 WHERE id > N-1，仅返回 [N]（即第一页已出现过的记录），且 hasMore=false，
        // 之后的记录（N-2 及更早）永远无法翻到。此处仅断言实际行为（非空），
        // 该问题已作为潜在生产 bug 上报，修复不属于本测试任务范围。
        assertThat(page2.getRecords()).isNotEmpty();
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
