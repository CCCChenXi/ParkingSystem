package com.xigeandwillian.parkingsystem.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xigeandwillian.parkingsystem.common.entity.Admin;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuthMapper extends BaseMapper<Admin> {
}
