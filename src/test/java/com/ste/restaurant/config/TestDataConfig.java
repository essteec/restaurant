package com.ste.restaurant.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

/**
 * Test configuration for database setup in integration tests.
 * Provides an embedded H2 database for fast, isolated testing.
 */
@TestConfiguration
@Profile("test")
public class TestDataConfig {

    /**
     * Creates an embedded H2 database configured to mimic PostgreSQL behavior.
     * This ensures our tests run consistently regardless of the actual database.
     */
    @Bean
    public DataSource testDataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("testdb")
                .build();
    }
}
