package com.xigeandwillian.parkingsystem.common.mapper;

import com.xigeandwillian.parkingsystem.admin.dto.parkinglot.LotSaveDTO;
import com.xigeandwillian.parkingsystem.admin.vo.parkinglot.LotListVO;
import com.xigeandwillian.parkingsystem.admin.vo.parkinglot.LotNameListVO;
import com.xigeandwillian.parkingsystem.client.vo.parkinglot.ParkingLotCache;
import com.xigeandwillian.parkingsystem.client.vo.parkinglot.ParkingLotVO;
import com.xigeandwillian.parkingsystem.common.entity.ParkingLot;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ParkingLotConverter {

    @Mapping(target = "totalSpots", ignore = true)
    @Mapping(target = "availableSpots", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    ParkingLot toEntity(LotSaveDTO dto);

    LotListVO toListVO(ParkingLot parkingLot);

    List<LotListVO> toListVOList(List<ParkingLot> parkingLots);

    LotNameListVO toNameListVO(ParkingLot parkingLot);

    List<LotNameListVO> toNameListVOList(List<ParkingLot> parkingLots);

    ParkingLotCache toCache(ParkingLot parkingLot);

    List<ParkingLotCache> toCacheList(List<ParkingLot> parkingLots);

    @Mapping(target = "distance", ignore = true)
    ParkingLotVO toVO(ParkingLot parkingLot);

    List<ParkingLotVO> toVOList(List<ParkingLot> parkingLots);

    @Mapping(target = "distance", ignore = true)
    ParkingLotVO cacheToVO(ParkingLotCache cache);

    List<ParkingLotVO> cacheToVOList(List<ParkingLotCache> caches);
}
