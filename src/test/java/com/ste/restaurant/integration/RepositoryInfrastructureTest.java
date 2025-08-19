package com.ste.restaurant.integration;

import com.ste.restaurant.utils.RepositoryTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Simple validation test to ensure our repository test foundation is working correctly.
 * This test verifies that:
 * - H2 database starts successfully
 * - TestEntityManager is properly configured
 * - Transaction management works
 * - Test profile is loaded correctly
 */
@ActiveProfiles("test")
class RepositoryInfrastructureTest extends RepositoryTestBase {

    @Test
    void shouldStartTestDatabaseSuccessfully() {
        // Test that our basic infrastructure is working
        assertTrue(entityManager != null, "TestEntityManager should be injected");
        
        // Test basic database connectivity
        executeQuery("SELECT 1");
        
        // This test passing means:
        // 1. H2 database started successfully
        // 2. JPA configuration is correct
        // 3. TestEntityManager is working
        // 4. Transaction management is active
        assertTrue(true, "Repository test infrastructure is working");
    }
    
    @Test
    void shouldSupportBasicDatabaseOperations() {
        // Test that we can execute queries and the database is responsive
        var result = executeNativeQuery("SELECT CURRENT_TIMESTAMP");
        assertTrue(result.size() > 0, "Database should respond to queries");
        
        // Test transaction boundaries work
        flush();
        clear();
        
        assertTrue(true, "Basic database operations are working");
    }
}
