package com.ste.restaurant.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;

/**
 * TestContainers configuration for integration tests that require a real PostgreSQL database.
 * This provides a PostgreSQL container that closely matches the production environment.
 * 
 * Note: This requires Docker to be installed and running.
 * Use @ActiveProfiles("testcontainers") to enable this configuration.
 * 
 * Currently disabled due to missing TestContainers dependencies.
 * To enable, add these dependencies to pom.xml:
 * - org.testcontainers:postgresql
 * - org.springframework.boot:spring-boot-testcontainers
 */
@TestConfiguration
@Profile("testcontainers")
public class TestContainerConfig {

    /**
     * TestContainers configuration placeholder.
     * 
     * To enable TestContainers, add the following dependencies to your pom.xml:
     * 
     * <dependency>
     *     <groupId>org.testcontainers</groupId>
     *     <artifactId>postgresql</artifactId>
     *     <scope>test</scope>
     * </dependency>
     * <dependency>
     *     <groupId>org.springframework.boot</groupId>
     *     <artifactId>spring-boot-testcontainers</artifactId>
     *     <scope>test</scope>
     * </dependency>
     * 
     * Then uncomment the PostgreSQL container configuration below.
     */

    /*
    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:15"))
                .withDatabaseName("restaurant_test")
                .withUsername("test_user")
                .withPassword("test_password")
                .withReuse(true)
                .withInitScript("test-schema.sql") // Optional: if you need schema initialization
                .withCommand(
                    "postgres",
                    "-c", "log_statement=all",
                    "-c", "log_min_duration_statement=0"
                );
    }
    */

    /**
     * Test properties for container-based testing.
     * These ensure we get detailed logging and proper schema management.
     */
    public static class ContainerTestProperties {
        public static final String[] JPA_PROPERTIES = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.show-sql=true",
                "spring.jpa.properties.hibernate.format_sql=true",
                "spring.jpa.properties.hibernate.use_sql_comments=true",
                "logging.level.org.hibernate.SQL=DEBUG",
                "logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE"
        };
    }
}
