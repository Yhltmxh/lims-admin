package com.shou.lims.organize.dept.controller;

import com.shou.lims.common.response.PageVO;
import com.shou.lims.common.response.Result;
import com.shou.lims.organize.dept.dto.DeptCreateDTO;
import com.shou.lims.organize.dept.dto.DeptQueryDTO;
import com.shou.lims.organize.dept.dto.DeptUpdateDTO;
import com.shou.lims.organize.dept.service.DeptService;
import com.shou.lims.organize.dept.vo.DeptVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
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
    @Operation(summary = "分页查询部门")
    @PreAuthorize("hasAuthority('organize:dept')")
    public Result<PageVO<DeptVO>> list(@Valid DeptQueryDTO query) {
        return Result.success(deptService.page(query));
    }

    @GetMapping("/tree")
    @Operation(summary = "获取部门树")
    @PreAuthorize("hasAuthority('organize:dept')")
    public Result<List<DeptVO>> tree() {
        return Result.success(deptService.getTree());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取部门详情")
    @PreAuthorize("hasAuthority('organize:dept')")
    public Result<DeptVO> getById(@PathVariable Long id) {
        return Result.success(deptService.getById(id));
    }

    @PostMapping
    @Operation(summary = "新增部门")
    @PreAuthorize("hasAuthority('organize:dept:add')")
    public Result<Long> create(@Valid @RequestBody DeptCreateDTO dto) {
        return Result.success(deptService.create(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑部门")
    @PreAuthorize("hasAuthority('organize:dept:edit')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody DeptUpdateDTO dto) {
        deptService.update(id, dto);
        return Result.success();
    }

    @DeleteMapping
    @Operation(summary = "批量删除部门")
    @PreAuthorize("hasAuthority('organize:dept:del')")
    public Result<Void> delete(@RequestBody @NotEmpty List<Long> ids) {
        deptService.delete(ids);
        return Result.success();
    }
}
