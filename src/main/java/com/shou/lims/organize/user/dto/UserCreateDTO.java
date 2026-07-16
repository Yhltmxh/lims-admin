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
    @Size(max = 32, message = "真实姓名不能超过32位")
    private String realName;

    @Phone
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Min(value = 0, message = "性别值不正确")
    @Max(value = 2, message = "性别值不正确")
    private Integer gender;

    @Positive(message = "部门ID必须为正数")
    private Long deptId;

    @NotEmpty(message = "角色不能为空")
    @Size(max = 100, message = "一次最多分配100个角色")
    private List<@Positive(message = "角色ID必须为正数") Long> roleIds;
}
