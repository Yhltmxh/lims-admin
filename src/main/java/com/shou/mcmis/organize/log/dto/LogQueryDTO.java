package com.shou.mcmis.organize.log.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class LogQueryDTO {
    /** 游标：上一页最后一条记录的ID，首次请求不传或传null */
    @Positive
    private Long lastId;

    @Min(1)
    @Max(200)
    private Integer pageSize = 20;

    private String username;
    private String module;
    private String action;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;
}
