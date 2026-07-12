package com.shou.lims.organize.user.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private Integer gender;
    private Integer status;
    private String deptName;
    private List<String> roleNames;
    private LocalDateTime createTime;
}
