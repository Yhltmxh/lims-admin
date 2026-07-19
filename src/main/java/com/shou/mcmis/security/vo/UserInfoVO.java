package com.shou.mcmis.security.vo;

import lombok.Data;
import java.util.List;
import java.time.LocalDateTime;

@Data
public class UserInfoVO {
    private Long userId;
    private String username;
    private String realName;
    private String avatar;
    private List<String> permissions;
    private List<String> roles;
    private LocalDateTime nextPermissionBoundary;
}
