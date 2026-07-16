package com.shou.lims.organize.dept.dto;

import com.shou.lims.common.validation.Phone;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DeptUpdateDTO {
    @PositiveOrZero(message = "父部门ID不能为负数")
    private Long parentId;

    @Size(max = 64, message = "部门名称不能超过64位")
    private String name;

    @Min(value = 0, message = "排序值不能为负数")
    private Integer sortOrder;

    @Size(max = 32, message = "负责人不能超过32位")
    private String leader;

    @Phone
    private String phone;

    @Min(value = 0, message = "状态值不正确")
    @Max(value = 1, message = "状态值不正确")
    private Integer status;

    @PositiveOrZero(message = "版本号不能为负数")
    private Integer version;
}
