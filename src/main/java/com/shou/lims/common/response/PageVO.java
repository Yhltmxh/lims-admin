package com.shou.lims.common.response;

import com.github.pagehelper.PageInfo;
import lombok.Data;
import java.util.List;

@Data
public class PageVO<T> {
    private List<T> records;
    private Long total;
    private Integer pageNum;
    private Integer pageSize;
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
