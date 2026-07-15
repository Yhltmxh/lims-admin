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
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final MenuMapper menuMapper;
    private final MenuConverter menuConverter;

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
        if (menu == null || StatusEnum.DISABLED.getValue().equals(menu.getStatus())) {
            throw new NotFoundException("菜单不存在");
        }
        return menuConverter.toVO(menu);
    }

    @Override
    @Transactional
    public Long create(MenuCreateDTO dto) {
        Menu existing = menuMapper.selectOne(new LambdaQueryWrapper<Menu>()
                .eq(Menu::getName, dto.getName()));
        if (existing != null) {
            throw new BusinessException(409, "菜单名称已存在");
        }
        Menu menu = menuConverter.toEntity(dto);
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
        menuMapper.deleteBatchIds(ids);
    }

    @Override
    public List<MenuVO> getTree() {
        List<Menu> allMenus = menuMapper.selectList(new LambdaQueryWrapper<Menu>()
                .eq(Menu::getStatus, StatusEnum.ENABLED.getValue())
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
}
