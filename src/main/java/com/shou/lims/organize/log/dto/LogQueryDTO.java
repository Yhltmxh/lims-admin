package com.shou.lims.organize.log.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LogQueryDTO {
    /** 游标：上一页最后一条记录的ID，首次请求不传或传null */
    private Long lastId;

    @Min(1)
    @Max(200)
    private Integer pageSize = 20;

    private String username;
    private String module;
    private String action;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
