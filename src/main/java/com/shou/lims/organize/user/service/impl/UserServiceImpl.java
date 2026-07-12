package com.shou.lims.organize.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.shou.lims.common.enums.StatusEnum;
import com.shou.lims.common.exception.BusinessException;
import com.shou.lims.common.exception.NotFoundException;
import com.shou.lims.common.response.PageVO;
import com.shou.lims.organize.user.converter.UserConverter;
import com.shou.lims.organize.user.dto.UserCreateDTO;
import com.shou.lims.organize.user.dto.UserQueryDTO;
import com.shou.lims.organize.user.dto.UserUpdateDTO;
import com.shou.lims.organize.user.entity.User;
import com.shou.lims.organize.user.mapper.UserMapper;
import com.shou.lims.organize.user.service.UserService;
import com.shou.lims.organize.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserConverter userConverter;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageVO<UserVO> page(UserQueryDTO query) {
        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .like(StringUtils.isNotBlank(query.getUsername()), User::getUsername, query.getUsername())
                .eq(query.getStatus() != null, User::getStatus, query.getStatus())
                .eq(query.getDeptId() != null, User::getDeptId, query.getDeptId())
                .eq(User::getIsDelete, 0)
                .orderByDesc(User::getCreateTime);
        List<User> list = userMapper.selectList(wrapper);
        PageInfo<User> pageInfo = new PageInfo<>(list);
        return PageVO.of(pageInfo.convert(userConverter::toVO));
    }

    @Override
    public UserVO getById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null || StatusEnum.DISABLED.getValue().equals(user.getStatus())) {
            throw new NotFoundException("用户不存在");
        }
        return userConverter.toVO(user);
    }

    @Override
    @Transactional
    public Long create(UserCreateDTO dto) {
        // Check username uniqueness
        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername())
                .eq(User::getIsDelete, 0));
        if (existing != null) {
            throw new BusinessException(409, "用户名已存在");
        }
        User user = userConverter.toEntity(dto);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setStatus(StatusEnum.ENABLED.getValue());
        userMapper.insert(user);

        // Assign roles
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            assignRoles(user.getId(), dto.getRoleIds());
        }
        return user.getId();
    }

    @Override
    @Transactional
    public void update(Long id, UserUpdateDTO dto) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new NotFoundException("用户不存在");
        }
        if (StringUtils.isNotBlank(dto.getRealName())) user.setRealName(dto.getRealName());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getGender() != null) user.setGender(dto.getGender());
        if (dto.getDeptId() != null) user.setDeptId(dto.getDeptId());
        if (dto.getStatus() != null) user.setStatus(dto.getStatus());
        userMapper.updateById(user);

        if (dto.getRoleIds() != null) {
            // Reassign roles: delete old, insert new
            assignRoles(id, dto.getRoleIds());
        }
    }

    @Override
    @Transactional
    public void delete(List<Long> ids) {
        userMapper.deleteBatchIds(ids); // MyBatis-Plus logic delete via @TableLogic
    }

    /**
     * Assign roles to a user.
     * TODO: This is a placeholder — needs a RoleAssignmentMapper or raw SQL for sys_user_role table.
     */
    private void assignRoles(Long userId, List<Long> roleIds) {
        // TODO: Replace with RoleAssignmentMapper or raw SQL for sys_user_role table
        // WARNING: Do NOT use userMapper.delete() here — that would logic-delete the user, not role assignments.
    }
}
