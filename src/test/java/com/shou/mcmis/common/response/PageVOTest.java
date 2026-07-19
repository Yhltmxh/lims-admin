package com.shou.mcmis.common.response;

import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.assertj.core.api.Assertions.assertThat;

class PageVOTest {

    @Test
    void shouldBuildFromPageInfo() {
        PageInfo<String> pageInfo = new PageInfo<>(Arrays.asList("a", "b", "c"));
        pageInfo.setTotal(10);
        pageInfo.setPageNum(2);
        pageInfo.setPageSize(3);

        PageVO<String> vo = PageVO.of(pageInfo);

        assertThat(vo.getRecords()).hasSize(3);
        assertThat(vo.getTotal()).isEqualTo(10);
        assertThat(vo.getPageNum()).isEqualTo(2);
        assertThat(vo.getPageSize()).isEqualTo(3);
        // PageInfo computes pages at construction time from the wrapped list (3 items, 1 page);
        // setTotal/setPageSize afterwards do NOT recompute pages, so actual value is 1.
        assertThat(vo.getTotalPages()).isEqualTo(1);
    }

    @Test
    void shouldHandleEmptyList() {
        PageInfo<String> pageInfo = new PageInfo<>(Collections.emptyList());
        pageInfo.setTotal(0);

        PageVO<String> vo = PageVO.of(pageInfo);

        assertThat(vo.getRecords()).isEmpty();
        assertThat(vo.getTotal()).isEqualTo(0);
        assertThat(vo.getTotalPages()).isEqualTo(0);
    }
}
