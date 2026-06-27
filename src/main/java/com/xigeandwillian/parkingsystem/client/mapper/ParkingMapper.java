package com.xigeandwillian.parkingsystem.client.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xigeandwillian.parkingsystem.common.entity.ParkingLot;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ParkingMapper extends BaseMapper<ParkingLot> {
}
