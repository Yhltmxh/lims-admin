package com.shou.lims.security.vo;

import lombok.Data;
import java.util.List;

@Data
public class UserInfoVO {
    private Long userId;
    private String username;
    private String realName;
    private String avatar;
    private List<String> permissions;
    private List<String> roles;
}
