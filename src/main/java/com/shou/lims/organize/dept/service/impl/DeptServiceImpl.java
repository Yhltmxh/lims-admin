package com.shou.lims.organize.dept.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.shou.lims.common.enums.StatusEnum;
import com.shou.lims.common.exception.BusinessException;
import com.shou.lims.common.exception.NotFoundException;
import com.shou.lims.common.response.PageVO;
import com.shou.lims.organize.dept.converter.DeptConverter;
import com.shou.lims.organize.dept.dto.DeptCreateDTO;
import com.shou.lims.organize.dept.dto.DeptQueryDTO;
import com.shou.lims.organize.dept.dto.DeptUpdateDTO;
import com.shou.lims.organize.dept.entity.Dept;
import com.shou.lims.organize.dept.mapper.DeptMapper;
import com.shou.lims.organize.dept.service.DeptService;
import com.shou.lims.organize.dept.vo.DeptVO;
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
public class DeptServiceImpl implements DeptService {

    private final DeptMapper deptMapper;
    private final DeptConverter deptConverter;

    @Override
    public PageVO<DeptVO> page(DeptQueryDTO query) {
        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<Dept> wrapper = new LambdaQueryWrapper<Dept>()
                .like(StringUtils.isNotBlank(query.getName()), Dept::getName, query.getName())
                .eq(query.getStatus() != null, Dept::getStatus, query.getStatus())
                .orderByAsc(Dept::getSortOrder);
        List<Dept> list = deptMapper.selectList(wrapper);
        PageInfo<Dept> pageInfo = new PageInfo<>(list);
        return PageVO.of(pageInfo.convert(deptConverter::toVO));
    }

    @Override
    public DeptVO getById(Long id) {
        Dept dept = deptMapper.selectById(id);
        if (dept == null || StatusEnum.DISABLED.getValue().equals(dept.getStatus())) {
            throw new NotFoundException("部门不存在");
        }
        return deptConverter.toVO(dept);
    }

    @Override
    @Transactional
    public Long create(DeptCreateDTO dto) {
        Dept existing = deptMapper.selectOne(new LambdaQueryWrapper<Dept>()
                .eq(Dept::getName, dto.getName()));
        if (existing != null) {
            throw new BusinessException(409, "部门名称已存在");
        }
        Dept dept = deptConverter.toEntity(dto);
        dept.setStatus(dto.getStatus() != null ? dto.getStatus() : StatusEnum.ENABLED.getValue());
        deptMapper.insert(dept);
        return dept.getId();
    }

    @Override
    @Transactional
    public void update(Long id, DeptUpdateDTO dto) {
        Dept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new NotFoundException("部门不存在");
        }
        if (dto.getParentId() != null) dept.setParentId(dto.getParentId());
        if (StringUtils.isNotBlank(dto.getName())) dept.setName(dto.getName());
        if (dto.getSortOrder() != null) dept.setSortOrder(dto.getSortOrder());
        if (dto.getLeader() != null) dept.setLeader(dto.getLeader());
        if (dto.getPhone() != null) dept.setPhone(dto.getPhone());
        if (dto.getStatus() != null) dept.setStatus(dto.getStatus());
        deptMapper.updateById(dept);
    }

    @Override
    @Transactional
    public void delete(List<Long> ids) {
        deptMapper.deleteBatchIds(ids);
    }

    @Override
    public List<DeptVO> getTree() {
        List<Dept> allDepts = deptMapper.selectList(new LambdaQueryWrapper<Dept>()
                .eq(Dept::getStatus, StatusEnum.ENABLED.getValue())
                .orderByAsc(Dept::getSortOrder));
        List<DeptVO> voList = deptConverter.toVOList(allDepts);
        Map<Long, List<DeptVO>> parentMap = voList.stream()
                .filter(d -> d.getParentId() != null)
                .collect(Collectors.groupingBy(DeptVO::getParentId));
        List<DeptVO> roots = new ArrayList<>();
        for (DeptVO vo : voList) {
            Long parentId = vo.getParentId();
            if (parentId == null || parentId == 0) {
                roots.add(vo);
            }
            vo.setChildren(parentMap.getOrDefault(vo.getId(), new ArrayList<>()));
        }
        return roots;
    }
}
