package com.shou.mcmis.organize.log.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LogVO {
    private Long id;
    private Long userId;
    private String username;
    private String module;
    private String action;
    private String method;
    private String params;
    private String result;
    private String ip;
    private Integer duration;
    private Integer status;
    private LocalDateTime createTime;
}
