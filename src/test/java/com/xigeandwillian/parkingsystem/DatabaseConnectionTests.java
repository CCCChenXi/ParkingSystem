package com.xigeandwillian.parkingsystem;

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

}
