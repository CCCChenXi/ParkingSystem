package com.xigeandwillian.parkingsystem.common.mapper;

import com.xigeandwillian.parkingsystem.admin.dto.auth.AdminCreateDTO;
import com.xigeandwillian.parkingsystem.admin.vo.auth.AdminListVO;
import com.xigeandwillian.parkingsystem.admin.vo.auth.AdminVO;
import com.xigeandwillian.parkingsystem.common.entity.Admin;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AdminConverter {

    AdminVO toVO(Admin admin);

    AdminListVO toListVO(Admin admin);

    List<AdminListVO> toListVOList(List<Admin> admins);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    Admin toEntity(AdminCreateDTO dto);
}
