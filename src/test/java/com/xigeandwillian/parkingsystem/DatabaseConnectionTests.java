package com.xigeandwillian.parkingsystem;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xigeandwillian.parkingsystem.admin.mapper.ParkingLotMapper;
import com.xigeandwillian.parkingsystem.admin.mapper.AdminParkingSpotMapper;
import com.xigeandwillian.parkingsystem.common.cache.ParkingCache;
import com.xigeandwillian.parkingsystem.common.entity.ParkingLot;
import com.xigeandwillian.parkingsystem.common.entity.ParkingSpot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class DatabaseConnectionTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ParkingCache parkingCache;
    @Autowired
    private ParkingLotMapper parkingLotMapper;
    @Autowired
    private AdminParkingSpotMapper adminParkingSpotMapper;

    @Test
    void testDatabaseConnection() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertEquals(1, result);
    }

    @Test
    void testJdbcTemplateIsNotNull() {
        assertNotNull(jdbcTemplate);
    }

    @Test
    void testDatabaseVersion() {
        String version = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
        System.out.println("MySQL Version: " + version);
        assertNotNull(version);
    }

    @Test
    void clearCache(){
        parkingCache.clearAll();
    }


    @Test
    void buildLot(){
        List<ParkingLot> parkingLots = parkingLotMapper.selectList(null);
        parkingLots.forEach(parkingLot -> {
            LambdaQueryWrapper<ParkingSpot> q = new LambdaQueryWrapper<>();
            q.eq(ParkingSpot::getLotId, parkingLot.getId());
            Long count = adminParkingSpotMapper.selectCount(q);
            System.out.println(parkingLot.getId()+' '+count);
            parkingLot.setTotalSpots(count.intValue());
            parkingLot.setAvailableSpots(count.intValue());
            parkingLotMapper.updateById(parkingLot);

        });
    }
}
