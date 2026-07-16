package com.shou.lims.organize.menu.controller;

import com.shou.lims.common.response.PageVO;
import com.shou.lims.common.response.Result;
import com.shou.lims.organize.menu.dto.MenuCreateDTO;
import com.shou.lims.organize.menu.dto.MenuQueryDTO;
import com.shou.lims.organize.menu.dto.MenuUpdateDTO;
import com.shou.lims.organize.menu.service.MenuService;
import com.shou.lims.organize.menu.vo.MenuVO;
import com.shou.lims.organize.log.annotation.Log;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/system/menus")
@RequiredArgsConstructor
@Tag(name = "菜单管理")
public class MenuController {

    private final MenuService menuService;

    @GetMapping
    @Operation(summary = "分页查询菜单")
    @PreAuthorize("hasAuthority('organize:menu:list')")
    public Result<PageVO<MenuVO>> list(@Valid MenuQueryDTO query) {
        return Result.success(menuService.page(query));
    }

    @GetMapping("/tree")
    @Operation(summary = "获取菜单树")
    @PreAuthorize("hasAuthority('organize:menu:list')")
    public Result<List<MenuVO>> tree() {
        return Result.success(menuService.getTree());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取菜单详情")
    @PreAuthorize("hasAuthority('organize:menu:list')")
    public Result<MenuVO> getById(@PathVariable @Positive Long id) {
        return Result.success(menuService.getById(id));
    }

    @PostMapping
    @Operation(summary = "新增菜单")
    @PreAuthorize("hasAuthority('organize:menu:add')")
    @Log(module = "菜单管理", action = "新增菜单")
    public Result<Long> create(@Valid @RequestBody MenuCreateDTO dto) {
        return Result.success(menuService.create(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑菜单")
    @PreAuthorize("hasAuthority('organize:menu:edit')")
    @Log(module = "菜单管理", action = "编辑菜单")
    public Result<Void> update(@PathVariable @Positive Long id, @Valid @RequestBody MenuUpdateDTO dto) {
        menuService.update(id, dto);
        return Result.success();
    }

    @DeleteMapping
    @Operation(summary = "批量删除菜单")
    @PreAuthorize("hasAuthority('organize:menu:del')")
    @Log(module = "菜单管理", action = "删除菜单")
    public Result<Void> delete(@Valid @RequestBody @NotEmpty @Size(max = 500)
                               List<@Positive Long> ids) {
        menuService.delete(ids);
        return Result.success();
    }
}
