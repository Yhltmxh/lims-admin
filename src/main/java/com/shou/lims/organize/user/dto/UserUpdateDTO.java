package com.shou.lims.organize.user.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserUpdateDTO {
    private String realName;
    private String phone;
    private String email;
    private Integer gender;
    private Long deptId;
    private Integer status;
    private List<Long> roleIds;
}
