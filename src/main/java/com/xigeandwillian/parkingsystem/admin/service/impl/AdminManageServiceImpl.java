package com.xigeandwillian.parkingsystem.admin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xigeandwillian.parkingsystem.admin.dto.auth.AdminCreateDTO;
import com.xigeandwillian.parkingsystem.admin.mapper.AuthMapper;
import com.xigeandwillian.parkingsystem.admin.service.AdminManageService;
import com.xigeandwillian.parkingsystem.admin.vo.auth.AdminListVO;
import com.xigeandwillian.parkingsystem.common.mapper.AdminConverter;
import com.xigeandwillian.parkingsystem.common.constant.ResultConstant;
import com.xigeandwillian.parkingsystem.common.entity.Admin;
import com.xigeandwillian.parkingsystem.common.exception.BusinessException;
import com.xigeandwillian.parkingsystem.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class AdminManageServiceImpl implements AdminManageService {

    private static final String ROLE_SUPER = "super";
    private static final String ROLE_OPERATOR = "operator";

    private final AdminConverter adminConverter;
    private final AuthMapper authMapper;

    @Override
    public Result list() {
        List<Admin> adminList = authMapper.selectList(null);
        List<AdminListVO> voList = adminList.stream().map(admin -> {
            return adminConverter.toListVO(admin);
        }).collect(Collectors.toList());
        return Result.ok(voList);
    }

    @Override
    public Result detail(Long id) {
        if (id == null) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "管理员ID不能为空");
        }
        Admin admin = authMapper.selectById(id);
        if (admin == null) {
            log.warn("管理员不存在: id={}", id);
            throw new BusinessException(ResultConstant.BAD_REQUEST, "管理员不存在");
        }
        AdminListVO vo = adminConverter.toListVO(admin);
        return Result.ok(vo);
    }

    @Override
    public Result create(AdminCreateDTO dto) {
        String role = dto.getRole();

        if (!ROLE_SUPER.equals(role) && !ROLE_OPERATOR.equals(role)) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "角色只能是 super 或 operator");
        }

        Admin exist = authMapper.selectOne(Wrappers.<Admin>lambdaQuery()
                .eq(Admin::getUsername, dto.getUsername()));
        if (exist != null) {
            throw new BusinessException(ResultConstant.BAD_REQUEST, "用户名已存在");
        }

        String encrypted = DigestUtils.md5DigestAsHex(
                dto.getPassword().getBytes(StandardCharsets.UTF_8));

        Admin admin = adminConverter.toEntity(dto);
        admin.setPassword(encrypted);
        authMapper.insert(admin);

        log.info("新增管理员成功: username={}, role={}, id={}", dto.getUsername(), role, admin.getId());
        return Result.ok();
    }
}
