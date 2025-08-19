package com.ste.restaurant.utils;

import com.ste.restaurant.config.TestDataConfig;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class for repository integration tests.
 * Provides common setup, configuration, and utility methods for all repository tests.
 * 
 * Extend this class for your repository tests to get:
 * - Configured test database
 * - TestEntityManager for manual entity management
 * - Transaction rollback after each test
 * - Common test utilities
 */
@DataJpaTest
@Import(TestDataConfig.class)
@ActiveProfiles("test")
@Transactional
public abstract class RepositoryTestBase {

    @Autowired
    protected TestEntityManager entityManager;

    /**
     * Common setup that runs before each test.
     * Override this method in subclasses to add repository-specific setup.
     */
    @BeforeEach
    void baseSetUp() {
        // Clear any existing data to ensure test isolation
        entityManager.clear();
        
        // Call subclass setup if implemented
        setUp();
    }

    /**
     * Template method for subclasses to implement their own setup logic.
     * This is called after the base setup is complete.
     */
    protected void setUp() {
        // Default implementation does nothing
        // Override in subclasses for custom setup
    }

    /**
     * Utility method to persist an entity and flush changes to database.
     * Useful for setting up test data that needs to be immediately available.
     * 
     * @param entity The entity to persist
     * @param <T> The entity type
     * @return The persisted entity with generated ID
     */
    protected <T> T persistAndFlush(T entity) {
        return entityManager.persistAndFlush(entity);
    }

    /**
     * Utility method to persist an entity without flushing.
     * Use this when you want to batch multiple operations.
     * 
     * @param entity The entity to persist
     * @param <T> The entity type
     * @return The persisted entity
     */
    protected <T> T persist(T entity) {
        entityManager.persist(entity);
        return entity;
    }

    /**
     * Utility method to flush pending changes to the database.
     * Call this after batching multiple persist operations.
     */
    protected void flush() {
        entityManager.flush();
    }

    /**
     * Utility method to clear the persistence context.
     * This detaches all managed entities, useful for testing lazy loading.
     */
    protected void clear() {
        entityManager.clear();
    }

    /**
     * Utility method to find an entity by ID.
     * 
     * @param entityClass The entity class
     * @param id The entity ID
     * @param <T> The entity type
     * @return The found entity or null
     */
    protected <T> T find(Class<T> entityClass, Object id) {
        return entityManager.find(entityClass, id);
    }

    /**
     * Utility method to refresh an entity from the database.
     * Useful for checking if changes were properly persisted.
     * 
     * @param entity The entity to refresh
     * @param <T> The entity type
     * @return The refreshed entity
     */
    protected <T> T refresh(T entity) {
        entityManager.refresh(entity);
        return entity;
    }

    /**
     * Utility method to execute a JPQL query for testing.
     * 
     * @param jpql The JPQL query string
     * @return Query result list
     */
    @SuppressWarnings("unchecked")
    protected <T> java.util.List<T> executeQuery(String jpql) {
        return entityManager.getEntityManager()
                .createQuery(jpql)
                .getResultList();
    }

    /**
     * Utility method to execute a native SQL query for testing.
     * 
     * @param sql The native SQL query string
     * @return Query result list
     */
    @SuppressWarnings("unchecked")
    protected java.util.List<Object[]> executeNativeQuery(String sql) {
        return entityManager.getEntityManager()
                .createNativeQuery(sql)
                .getResultList();
    }

    /**
     * Utility method to count all entities of a given type.
     * Useful for verifying data setup or cleanup.
     * 
     * @param entityClass The entity class
     * @param <T> The entity type
     * @return The count of entities
     */
    protected <T> long countEntities(Class<T> entityClass) {
        String jpql = "SELECT COUNT(e) FROM " + entityClass.getSimpleName() + " e";
        return (Long) entityManager.getEntityManager()
                .createQuery(jpql)
                .getSingleResult();
    }

    /**
     * Utility method to verify that an entity exists in the database.
     * 
     * @param entityClass The entity class
     * @param id The entity ID
     * @param <T> The entity type
     * @return true if entity exists, false otherwise
     */
    protected <T> boolean entityExists(Class<T> entityClass, Object id) {
        return find(entityClass, id) != null;
    }

    /**
     * Utility method to delete all entities of a given type.
     * Useful for test cleanup or isolation.
     * 
     * @param entityClass The entity class
     * @param <T> The entity type
     */
    protected <T> void deleteAllEntities(Class<T> entityClass) {
        String jpql = "DELETE FROM " + entityClass.getSimpleName();
        entityManager.getEntityManager()
                .createQuery(jpql)
                .executeUpdate();
    }
}
