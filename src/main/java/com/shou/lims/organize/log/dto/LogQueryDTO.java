package com.shou.lims.organize.log.dto;

import com.shou.lims.common.response.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class LogQueryDTO extends PageQuery {
    private String username;
    private String module;
    private String action;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
