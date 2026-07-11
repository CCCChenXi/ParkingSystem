package com.xigeandwillian.parkingsystem.common.mapper;

import com.xigeandwillian.parkingsystem.client.dto.user.VehicleDTO;
import com.xigeandwillian.parkingsystem.common.entity.Vehicle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface VehicleConverter {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    Vehicle toEntity(VehicleDTO dto);

    List<Vehicle> toEntityList(List<VehicleDTO> dtos);
}
