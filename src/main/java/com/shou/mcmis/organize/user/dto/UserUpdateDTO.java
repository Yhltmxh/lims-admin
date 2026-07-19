package com.shou.mcmis.organize.user.dto;

import com.shou.mcmis.common.validation.Phone;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class UserUpdateDTO {
    @Size(max = 32, message = "真实姓名不能超过32位")
    private String realName;

    @Phone
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 64, message = "邮箱不能超过64位")
    private String email;

    @Min(value = 0, message = "性别值不正确")
    @Max(value = 2, message = "性别值不正确")
    private Integer gender;

    @Positive(message = "部门ID必须为正数")
    private Long deptId;

    @Min(value = 0, message = "状态值不正确")
    @Max(value = 1, message = "状态值不正确")
    private Integer status;

    @Size(max = 100, message = "一次最多分配100个角色")
    private List<@Positive(message = "角色ID必须为正数") Long> roleIds;

    @PositiveOrZero(message = "版本号不能为负数")
    private Integer version;
}
