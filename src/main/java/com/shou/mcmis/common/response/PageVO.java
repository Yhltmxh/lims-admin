package com.shou.mcmis.common.response;

import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
public class PageVO<T> {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<T> records;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long total;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer pageNum;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer pageSize;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer totalPages;

    public static <T> PageVO<T> of(PageInfo<T> pageInfo) {
        PageVO<T> vo = new PageVO<>();
        vo.setRecords(pageInfo.getList());
        vo.setTotal(pageInfo.getTotal());
        vo.setPageNum(pageInfo.getPageNum());
        vo.setPageSize(pageInfo.getPageSize());
        vo.setTotalPages(pageInfo.getPages());
        return vo;
    }
}
