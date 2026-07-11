package com.xigeandwillian.parkingsystem.common.mapper;

import com.xigeandwillian.parkingsystem.admin.dto.parkingspot.SpotUpdateDTO;
import com.xigeandwillian.parkingsystem.common.entity.ParkingSpot;
import com.xigeandwillian.parkingsystem.common.vo.parkingspot.SpotListVO;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ParkingSpotConverter {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lotId", ignore = true)
    @Mapping(target = "seq", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    ParkingSpot toEntity(SpotUpdateDTO dto);

    @Mapping(target = "status", ignore = true)
    SpotListVO toListVO(ParkingSpot parkingSpot);

    List<SpotListVO> toListVOList(List<ParkingSpot> parkingSpots);

    @AfterMapping
    default void afterToListVO(ParkingSpot parkingSpot, @MappingTarget SpotListVO vo) {
        vo.setStatus(0);
    }
}
