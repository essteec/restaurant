package com.ste.restaurant.integration;

import com.ste.restaurant.entity.enums.TableStatus;
import com.ste.restaurant.entity.TableTop;
import com.ste.restaurant.repository.TableTopRepository;
import com.ste.restaurant.utils.DatabaseTestUtils;
import com.ste.restaurant.utils.RepositoryTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for TableTopRepository.
 * Tests the repository layer with real database operations using H2 in-memory database.
 */
@DataJpaTest
@DisplayName("TableTop Repository Integration Tests")
class TableTopRepositoryIntegrationTest extends RepositoryTestBase {

    @Autowired
    private TableTopRepository tableTopRepository;

    // Test entities
    private TableTop testTable1;
    private TableTop testTable2;
    private TableTop testTable3;

    @Override
    protected void setUp() {
        setupTestData();
    }

    private void setupTestData() {
        // Create test tables with different capacities and statuses
        testTable1 = DatabaseTestUtils.createTestTable(1, 4);  // Creates "T01"
        testTable2 = DatabaseTestUtils.createTestTable(2, 6);  // Creates "T02"
        testTable3 = DatabaseTestUtils.createTestTable(3, 2);  // Creates "T03"

        // Persist test entities
        persistAndFlush(testTable1);
        persistAndFlush(testTable2);
        persistAndFlush(testTable3);
    }

    @Nested
    @DisplayName("Basic CRUD Operations")
    class BasicCrudTests {

        @Test
        @DisplayName("Should save and retrieve table successfully")
        void shouldSaveAndRetrieveTable() {
            // Given
            TableTop newTable = DatabaseTestUtils.createTestTable(4, 8);  // Creates "T04"
            
            // When
            TableTop savedTable = tableTopRepository.save(newTable);
            Optional<TableTop> retrievedTable = tableTopRepository.findById(savedTable.getTableId());
            
            // Then
            assertThat(retrievedTable).isPresent();
            assertThat(retrievedTable.get().getTableNumber()).isEqualTo("T04");
            assertThat(retrievedTable.get().getCapacity()).isEqualTo(8);
            assertThat(retrievedTable.get().getTableId()).isNotNull();
        }

        @Test
        @DisplayName("Should update table successfully")
        void shouldUpdateTable() {
            // Given
            TableTop table = testTable1;
            String originalTableNumber = table.getTableNumber();
            
            // When
            table.setCapacity(6);
            TableTop updatedTable = tableTopRepository.save(table);
            
            // Then
            assertThat(updatedTable.getTableId()).isEqualTo(table.getTableId());
            assertThat(updatedTable.getTableNumber()).isEqualTo(originalTableNumber);
            assertThat(updatedTable.getCapacity()).isEqualTo(6);
        }

        @Test
        @DisplayName("Should delete table successfully")
        void shouldDeleteTable() {
            // Given
            TableTop table = testTable3;
            Long tableId = table.getTableId();
            
            // When
            tableTopRepository.delete(table);
            flush();
            
            // Then
            Optional<TableTop> deletedTable = tableTopRepository.findById(tableId);
            assertThat(deletedTable).isEmpty();
        }

        @Test
        @DisplayName("Should find all tables")
        void shouldFindAllTables() {
            // When
            List<TableTop> allTables = tableTopRepository.findAll();
            
            // Then
            assertThat(allTables).hasSize(3);
            assertThat(allTables)
                .extracting(TableTop::getTableNumber)
                .containsExactlyInAnyOrder("T01", "T02", "T03");
        }
    }

    @Nested
    @DisplayName("Custom Query Tests")
    class CustomQueryTests {

        @Test
        @DisplayName("Should find table by table number")
        void shouldFindByTableNumber() {
            // When
            Optional<TableTop> foundTable = tableTopRepository.findByTableNumber("T01");
            
            // Then
            assertThat(foundTable).isPresent();
            assertThat(foundTable.get().getTableNumber()).isEqualTo("T01");
            assertThat(foundTable.get().getCapacity()).isEqualTo(4);
        }

        @Test
        @DisplayName("Should return empty when table number not found")
        void shouldReturnEmptyWhenTableNumberNotFound() {
            // When
            Optional<TableTop> foundTable = tableTopRepository.findByTableNumber("T999");
            
            // Then
            assertThat(foundTable).isEmpty();
        }

        @Test
        @DisplayName("Should find tables by status")
        void shouldFindByStatus() {
            // When
            List<TableTop> availableTables = tableTopRepository.findAllByTableStatus(TableStatus.AVAILABLE);
            
            // Then
            assertThat(availableTables).hasSize(3); // All test tables are AVAILABLE by default
            assertThat(availableTables)
                .extracting(TableTop::getTableNumber)
                .containsExactlyInAnyOrder("T01", "T02", "T03");
        }

        @Test
        @DisplayName("Should check if table exists by table number")
        void shouldCheckIfTableExistsByTableNumber() {
            // When & Then
            assertThat(tableTopRepository.existsTableTopByTableNumber("T01")).isTrue();
            assertThat(tableTopRepository.existsTableTopByTableNumber("T02")).isTrue();
            assertThat(tableTopRepository.existsTableTopByTableNumber("T03")).isTrue();
            assertThat(tableTopRepository.existsTableTopByTableNumber("T999")).isFalse();
        }
    }

    @Nested
    @DisplayName("Data Integrity Tests")
    class DataIntegrityTests {

        @Test
        @DisplayName("Should handle unique table number constraint")
        void shouldHandleUniqueTableNumberConstraint() {
            // Given
            TableTop duplicateTable = DatabaseTestUtils.createTestTable(1, 8); // Same number as testTable1
            
            // When & Then
            try {
                tableTopRepository.save(duplicateTable);
                flush();
                // If we reach here without exception, the constraint might not be enforced
                // Let's check that only one table with this number exists
                List<TableTop> tablesWithSameNumber = tableTopRepository.findAll()
                    .stream()
                    .filter(table -> "T01".equals(table.getTableNumber()))
                    .toList();
                assertThat(tablesWithSameNumber).hasSize(1);
            } catch (Exception e) {
                // Expected behavior if unique constraint is enforced
                assertThat(e).isNotNull();
            }
        }

        @Test
        @DisplayName("Should handle capacity validation")
        void shouldHandleCapacityValidation() {
            // Given
            TableTop table = testTable1;
            
            // When & Then - Test positive capacity
            table.setCapacity(10);
            TableTop savedTable = tableTopRepository.save(table);
            assertThat(savedTable.getCapacity()).isEqualTo(10);
            
            // Test zero capacity (should be handled by business logic)
            table.setCapacity(0);
            TableTop zeroCapacityTable = tableTopRepository.save(table);
            assertThat(zeroCapacityTable.getCapacity()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle empty result sets gracefully")
        void shouldHandleEmptyResultSetsGracefully() {
            // When - querying for non-existent data
            Optional<TableTop> nonExistentTable = tableTopRepository.findByTableNumber("NONEXISTENT");
            List<TableTop> occupiedTables = tableTopRepository.findAllByTableStatus(TableStatus.OCCUPIED);
            
            // Then
            assertThat(nonExistentTable).isEmpty();
            assertThat(occupiedTables).isEmpty(); // No tables are occupied in our test data
        }

        @Test
        @DisplayName("Should handle different table statuses")
        void shouldHandleDifferentTableStatuses() {
            // Given - Update one table to be occupied
            testTable1.setTableStatus(TableStatus.OCCUPIED);
            tableTopRepository.save(testTable1);
            flush();
            
            // When - query by different statuses
            List<TableTop> availableTables = tableTopRepository.findAllByTableStatus(TableStatus.AVAILABLE);
            List<TableTop> occupiedTables = tableTopRepository.findAllByTableStatus(TableStatus.OCCUPIED);
            
            // Then
            assertThat(availableTables).hasSize(2);
            assertThat(occupiedTables).hasSize(1);
            assertThat(occupiedTables.get(0).getTableNumber()).isEqualTo("T01");
        }

        @Test
        @DisplayName("Should maintain data consistency after multiple operations")
        void shouldMaintainDataConsistencyAfterMultipleOperations() {
            // Given
            int initialCount = tableTopRepository.findAll().size();
            
            // When - perform multiple operations
            TableTop newTable1 = DatabaseTestUtils.createTestTable(5, 5); // Creates "T05"
            TableTop newTable2 = DatabaseTestUtils.createTestTable(6, 3); // Creates "T06"
            
            tableTopRepository.save(newTable1);
            tableTopRepository.save(newTable2);
            
            // Delete one existing table
            tableTopRepository.delete(testTable2);
            flush();
            
            // Then
            List<TableTop> finalTables = tableTopRepository.findAll();
            assertThat(finalTables).hasSize(initialCount + 1); // +2 new, -1 deleted
            
            // Verify specific tables exist/don't exist
            assertThat(tableTopRepository.findByTableNumber("T05")).isPresent();
            assertThat(tableTopRepository.findByTableNumber("T06")).isPresent();
            assertThat(tableTopRepository.findByTableNumber("T02")).isEmpty();
        }
    }
}
