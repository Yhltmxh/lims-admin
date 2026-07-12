package com.shou.lims.organize.user.dto;

import com.shou.lims.common.validation.Phone;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class UserCreateDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 32, message = "用户名长度3-32位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度6-32位")
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    @Phone
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    private Integer gender;
    private Long deptId;

    @NotEmpty(message = "角色不能为空")
    private List<Long> roleIds;
}
