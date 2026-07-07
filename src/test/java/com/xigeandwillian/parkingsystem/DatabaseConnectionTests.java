package com.xigeandwillian.parkingsystem;

import com.xigeandwillian.parkingsystem.admin.mapper.AdminParkingSpotMapper;
import com.xigeandwillian.parkingsystem.common.cache.ParkingCache;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class DatabaseConnectionTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ParkingCache parkingCache;
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


}
