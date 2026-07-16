package com.shou.lims.organize.dept.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DeptVO {
    private Long id;
    private Long parentId;
    private String name;
    private Integer sortOrder;
    private String leader;
    private String phone;
    private Integer status;
    private Integer version;
    private List<DeptVO> children;
    private LocalDateTime createTime;
}
