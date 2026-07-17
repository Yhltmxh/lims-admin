package com.shou.lims.organize.dept.controller;

import com.shou.lims.common.response.PageVO;
import com.shou.lims.common.response.Result;
import com.shou.lims.organize.dept.dto.DeptCreateDTO;
import com.shou.lims.organize.dept.dto.DeptQueryDTO;
import com.shou.lims.organize.dept.dto.DeptUpdateDTO;
import com.shou.lims.organize.dept.service.DeptService;
import com.shou.lims.organize.dept.vo.DeptVO;
import com.shou.lims.organize.log.annotation.Log;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/system/depts")
@RequiredArgsConstructor
@Tag(name = "部门管理")
public class DeptController {

    private final DeptService deptService;

    @GetMapping
    @Operation(summary = "分页查询部门", operationId = "listDepartments")
    @PreAuthorize("hasAuthority('organize:dept:list')")
    public Result<PageVO<DeptVO>> list(@Valid @ParameterObject DeptQueryDTO query) {
        return Result.success(deptService.page(query));
    }

    @GetMapping("/tree")
    @Operation(summary = "获取部门树", operationId = "getDepartmentTree")
    @PreAuthorize("hasAuthority('organize:dept:list')")
    public Result<List<DeptVO>> tree() {
        return Result.success(deptService.getTree());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取部门详情", operationId = "getDepartmentById")
    @PreAuthorize("hasAuthority('organize:dept:list')")
    public Result<DeptVO> getById(@PathVariable @Positive Long id) {
        return Result.success(deptService.getById(id));
    }

    @PostMapping
    @Operation(summary = "新增部门", operationId = "createDepartment")
    @PreAuthorize("hasAuthority('organize:dept:add')")
    @Log(module = "部门管理", action = "新增部门")
    public Result<Long> create(@Valid @RequestBody DeptCreateDTO dto) {
        return Result.success(deptService.create(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑部门", operationId = "updateDepartment")
    @PreAuthorize("hasAuthority('organize:dept:edit')")
    @Log(module = "部门管理", action = "编辑部门")
    public Result<Void> update(@PathVariable @Positive Long id, @Valid @RequestBody DeptUpdateDTO dto) {
        deptService.update(id, dto);
        return Result.success();
    }

    @DeleteMapping
    @Operation(summary = "批量删除部门", operationId = "deleteDepartments")
    @PreAuthorize("hasAuthority('organize:dept:del')")
    @Log(module = "部门管理", action = "删除部门")
    public Result<Void> delete(@Valid @RequestBody @NotEmpty @Size(max = 500)
                               List<@Positive Long> ids) {
        deptService.delete(ids);
        return Result.success();
    }
}
