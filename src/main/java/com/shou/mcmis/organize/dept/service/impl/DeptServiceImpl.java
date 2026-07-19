package com.shou.mcmis.organize.dept.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.shou.mcmis.common.enums.StatusEnum;
import com.shou.mcmis.common.exception.BusinessException;
import com.shou.mcmis.common.exception.NotFoundException;
import com.shou.mcmis.common.response.PageVO;
import com.shou.mcmis.organize.dept.converter.DeptConverter;
import com.shou.mcmis.organize.dept.dto.DeptCreateDTO;
import com.shou.mcmis.organize.dept.dto.DeptQueryDTO;
import com.shou.mcmis.organize.dept.dto.DeptUpdateDTO;
import com.shou.mcmis.organize.dept.entity.Dept;
import com.shou.mcmis.organize.dept.mapper.DeptMapper;
import com.shou.mcmis.organize.dept.service.DeptService;
import com.shou.mcmis.organize.dept.vo.DeptVO;
import com.shou.mcmis.organize.user.entity.User;
import com.shou.mcmis.organize.user.mapper.UserMapper;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeptServiceImpl implements DeptService {

    private final DeptMapper deptMapper;
    private final DeptConverter deptConverter;
    private final UserMapper userMapper;

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
        if (dept == null) {
            throw new NotFoundException("部门不存在");
        }
        return deptConverter.toVO(dept);
    }

    @Override
    @Transactional
    public Long create(DeptCreateDTO dto) {
        validateParent(null, dto.getParentId());
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
        if (dto.getVersion() != null && !dto.getVersion().equals(dept.getVersion())) {
            throw new BusinessException(409, "数据已被其他用户修改，请刷新后重试");
        }
        if (dto.getParentId() != null) {
            validateParent(id, dto.getParentId());
        }
        if (StringUtils.isNotBlank(dto.getName())) {
            Dept duplicate = deptMapper.selectOne(new LambdaQueryWrapper<Dept>()
                    .eq(Dept::getName, dto.getName()).ne(Dept::getId, id));
            if (duplicate != null) {
                throw new BusinessException(409, "部门名称已存在");
            }
        }
        if (StatusEnum.DISABLED.getValue().equals(dto.getStatus())
                && deptMapper.selectCount(new LambdaQueryWrapper<Dept>()
                .eq(Dept::getParentId, id)
                .eq(Dept::getStatus, StatusEnum.ENABLED.getValue())) > 0) {
            throw new BusinessException(400, "请先禁用子部门");
        }
        if (dto.getParentId() != null) dept.setParentId(dto.getParentId());
        if (StringUtils.isNotBlank(dto.getName())) dept.setName(dto.getName());
        if (dto.getSortOrder() != null) dept.setSortOrder(dto.getSortOrder());
        if (dto.getLeader() != null) dept.setLeader(dto.getLeader());
        if (dto.getPhone() != null) dept.setPhone(dto.getPhone());
        if (dto.getStatus() != null) dept.setStatus(dto.getStatus());
        if (deptMapper.updateById(dept) == 0) {
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
        long childCount = deptMapper.selectCount(new LambdaQueryWrapper<Dept>()
                .in(Dept::getParentId, distinctIds)
                .notIn(Dept::getId, distinctIds));
        if (childCount > 0) {
            throw new BusinessException(400, "存在未同时删除的子部门");
        }
        long userCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .in(User::getDeptId, distinctIds));
        if (userCount > 0) {
            throw new BusinessException(400, "部门下存在用户，不能删除");
        }
        deptMapper.deleteBatchIds(distinctIds);
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

    private void validateParent(Long currentId, Long parentId) {
        if (parentId == null || parentId == 0L) {
            return;
        }
        if (parentId.equals(currentId)) {
            throw new BusinessException(400, "上级部门不能是自身");
        }
        Set<Long> visited = new HashSet<>();
        Long cursor = parentId;
        while (cursor != null && cursor != 0L) {
            if (!visited.add(cursor) || cursor.equals(currentId)) {
                throw new BusinessException(400, "部门层级不能形成循环");
            }
            Dept parent = deptMapper.selectById(cursor);
            if (parent == null) {
                throw new BusinessException(400, "上级部门不存在");
            }
            if (!StatusEnum.ENABLED.getValue().equals(parent.getStatus())) {
                throw new BusinessException(400, "上级部门已禁用");
            }
            cursor = parent.getParentId();
        }
    }
}
