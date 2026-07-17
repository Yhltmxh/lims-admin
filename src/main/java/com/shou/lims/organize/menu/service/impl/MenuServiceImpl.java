package com.shou.lims.organize.menu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.shou.lims.common.enums.StatusEnum;
import com.shou.lims.common.exception.BusinessException;
import com.shou.lims.common.exception.NotFoundException;
import com.shou.lims.common.response.PageVO;
import com.shou.lims.organize.menu.converter.MenuConverter;
import com.shou.lims.organize.menu.dto.MenuCreateDTO;
import com.shou.lims.organize.menu.dto.MenuQueryDTO;
import com.shou.lims.organize.menu.dto.MenuUpdateDTO;
import com.shou.lims.organize.menu.entity.Menu;
import com.shou.lims.organize.menu.mapper.MenuMapper;
import com.shou.lims.organize.menu.service.MenuService;
import com.shou.lims.organize.menu.vo.MenuVO;
import com.shou.lims.organize.menu.vo.MenuRouteVO;
import com.shou.lims.organize.role.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final MenuMapper menuMapper;
    private final MenuConverter menuConverter;
    private final RoleMapper roleMapper;

    @Override
    public PageVO<MenuVO> page(MenuQueryDTO query) {
        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<Menu>()
                .like(StringUtils.isNotBlank(query.getName()), Menu::getName, query.getName())
                .eq(query.getStatus() != null, Menu::getStatus, query.getStatus())
                .orderByAsc(Menu::getSortOrder);
        List<Menu> list = menuMapper.selectList(wrapper);
        PageInfo<Menu> pageInfo = new PageInfo<>(list);
        return PageVO.of(pageInfo.convert(menuConverter::toVO));
    }

    @Override
    public MenuVO getById(Long id) {
        Menu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new NotFoundException("菜单不存在");
        }
        return menuConverter.toVO(menu);
    }

    @Override
    @Transactional
    public Long create(MenuCreateDTO dto) {
        validateParent(null, dto.getParentId());
        Long parentId = dto.getParentId() == null ? 0L : dto.getParentId();
        Menu existing = menuMapper.selectOne(new LambdaQueryWrapper<Menu>()
                .eq(Menu::getParentId, parentId)
                .eq(Menu::getName, dto.getName()));
        if (existing != null) {
            throw new BusinessException(409, "菜单名称已存在");
        }
        Menu menu = menuConverter.toEntity(dto);
        menu.setParentId(parentId);
        menu.setStatus(dto.getStatus() != null ? dto.getStatus() : StatusEnum.ENABLED.getValue());
        menuMapper.insert(menu);
        return menu.getId();
    }

    @Override
    @Transactional
    public void update(Long id, MenuUpdateDTO dto) {
        Menu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new NotFoundException("菜单不存在");
        }
        if (dto.getVersion() != null && !dto.getVersion().equals(menu.getVersion())) {
            throw new BusinessException(409, "数据已被其他用户修改，请刷新后重试");
        }
        if (dto.getParentId() != null) {
            validateParent(id, dto.getParentId());
        }
        if (StringUtils.isNotBlank(dto.getName()) || dto.getParentId() != null) {
            Long parentId = dto.getParentId() != null ? dto.getParentId() : menu.getParentId();
            String name = StringUtils.isNotBlank(dto.getName()) ? dto.getName() : menu.getName();
            Menu duplicate = menuMapper.selectOne(new LambdaQueryWrapper<Menu>()
                    .eq(Menu::getParentId, parentId)
                    .eq(Menu::getName, name)
                    .ne(Menu::getId, id));
            if (duplicate != null) {
                throw new BusinessException(409, "菜单名称已存在");
            }
        }
        if (StatusEnum.DISABLED.getValue().equals(dto.getStatus())
                && menuMapper.selectCount(new LambdaQueryWrapper<Menu>()
                .eq(Menu::getParentId, id)
                .eq(Menu::getStatus, StatusEnum.ENABLED.getValue())) > 0) {
            throw new BusinessException(400, "请先禁用子菜单");
        }
        if (dto.getParentId() != null) menu.setParentId(dto.getParentId());
        if (StringUtils.isNotBlank(dto.getName())) menu.setName(dto.getName());
        if (dto.getPath() != null) menu.setPath(dto.getPath());
        if (dto.getComponent() != null) menu.setComponent(dto.getComponent());
        if (dto.getIcon() != null) menu.setIcon(dto.getIcon());
        if (dto.getSortOrder() != null) menu.setSortOrder(dto.getSortOrder());
        if (dto.getHidden() != null) menu.setHidden(dto.getHidden());
        if (dto.getStatus() != null) menu.setStatus(dto.getStatus());
        if (menuMapper.updateById(menu) == 0) {
            throw new BusinessException(409, "数据已被其他用户修改，请刷新后重试");
        }
    }

    @Override
    @Transactional
    public void delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<Long> distinctIds = new ArrayList<>(new LinkedHashSet<>(ids));
        long childCount = menuMapper.selectCount(new LambdaQueryWrapper<Menu>()
                .in(Menu::getParentId, distinctIds)
                .notIn(Menu::getId, distinctIds));
        if (childCount > 0) {
            throw new BusinessException(400, "存在未同时删除的子菜单");
        }
        roleMapper.deleteRoleMenusByMenuIds(distinctIds);
        menuMapper.deleteBatchIds(distinctIds);
    }

    @Override
    public List<MenuVO> getTree() {
        List<Menu> allMenus = menuMapper.selectList(new LambdaQueryWrapper<Menu>()
                .orderByAsc(Menu::getSortOrder));
        List<MenuVO> voList = menuConverter.toVOList(allMenus);
        Map<Long, List<MenuVO>> parentMap = voList.stream()
                .filter(m -> m.getParentId() != null)
                .collect(Collectors.groupingBy(MenuVO::getParentId));
        List<MenuVO> roots = new ArrayList<>();
        for (MenuVO vo : voList) {
            Long parentId = vo.getParentId();
            if (parentId == null || parentId == 0) {
                roots.add(vo);
            }
            vo.setChildren(parentMap.getOrDefault(vo.getId(), new ArrayList<>()));
        }
        return roots;
    }

    @Override
    public List<MenuRouteVO> getCurrentUserMenuTree(Long userId) {
        List<Menu> assignedMenus = menuMapper.selectByUserId(userId);
        if (assignedMenus.isEmpty()) {
            return List.of();
        }

        List<Menu> enabledMenus = menuMapper.selectList(new LambdaQueryWrapper<Menu>()
                .eq(Menu::getStatus, StatusEnum.ENABLED.getValue())
                .orderByAsc(Menu::getSortOrder));
        Map<Long, Menu> menuMap = enabledMenus.stream()
                .collect(Collectors.toMap(Menu::getId, Function.identity()));

        Set<Long> visibleIds = new HashSet<>();
        for (Menu assignedMenu : assignedMenus) {
            Menu current = assignedMenu;
            while (current != null && visibleIds.add(current.getId())) {
                Long parentId = current.getParentId();
                current = parentId == null || parentId == 0L ? null : menuMap.get(parentId);
            }
        }

        List<Menu> visibleMenus = enabledMenus.stream()
                .filter(menu -> visibleIds.contains(menu.getId()))
                .toList();
        return buildRouteTree(visibleMenus);
    }

    private MenuRouteVO toRouteVO(Menu menu) {
        MenuRouteVO route = new MenuRouteVO();
        route.setKey(String.valueOf(menu.getId()));
        route.setName(menu.getName());
        route.setPath(menu.getPath());
        route.setIcon(menu.getIcon());
        route.setHideInMenu(Integer.valueOf(1).equals(menu.getHidden()));
        route.setChildren(new ArrayList<>());
        return route;
    }

    private List<MenuRouteVO> buildRouteTree(List<Menu> menus) {
        Map<Long, MenuRouteVO> routeMap = menus.stream()
                .collect(Collectors.toMap(Menu::getId, this::toRouteVO));
        List<MenuRouteVO> roots = new ArrayList<>();
        for (Menu menu : menus) {
            MenuRouteVO route = routeMap.get(menu.getId());
            Long parentId = menu.getParentId();
            MenuRouteVO parent = parentId == null ? null : routeMap.get(parentId);
            if (parent == null || parentId == 0L) {
                roots.add(route);
            } else {
                parent.getChildren().add(route);
            }
        }
        return roots;
    }

    private void validateParent(Long currentId, Long parentId) {
        if (parentId == null || parentId == 0L) {
            return;
        }
        if (parentId.equals(currentId)) {
            throw new BusinessException(400, "上级菜单不能是自身");
        }
        Set<Long> visited = new HashSet<>();
        Long cursor = parentId;
        while (cursor != null && cursor != 0L) {
            if (!visited.add(cursor) || cursor.equals(currentId)) {
                throw new BusinessException(400, "菜单层级不能形成循环");
            }
            Menu parent = menuMapper.selectById(cursor);
            if (parent == null) {
                throw new BusinessException(400, "上级菜单不存在");
            }
            if (!StatusEnum.ENABLED.getValue().equals(parent.getStatus())) {
                throw new BusinessException(400, "上级菜单已禁用");
            }
            cursor = parent.getParentId();
        }
    }
}
