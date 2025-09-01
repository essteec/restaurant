package com.ste.restaurant.integration;

import com.ste.restaurant.entity.CallRequest;
import com.ste.restaurant.entity.enums.RequestType;
import com.ste.restaurant.entity.enums.TableStatus;
import com.ste.restaurant.entity.TableTop;
import com.ste.restaurant.entity.User;
import com.ste.restaurant.entity.enums.UserRole;
import com.ste.restaurant.repository.CallRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for CallRequestRepository.
 * Tests the repository layer with real database operations using H2 in-memory database.
 */
@DataJpaTest
@DisplayName("CallRequest Repository Integration Tests")
class CallRequestRepositoryIntegrationTest {

    @Autowired
    private CallRequestRepository callRequestRepository;

    @Autowired
    private TestEntityManager entityManager;

    // Test data
    private CallRequest testCallRequest1;
    private CallRequest testCallRequest2;
    private CallRequest testCallRequest3;
    private User testCustomer1;
    private User testCustomer2;
    private TableTop testTable1;
    private TableTop testTable2;

    @BeforeEach
    void setUp() {
        setupTestData();
    }

    void setupTestData() {
        // Create test users
        testCustomer1 = createTestUser("John", "Doe", "john.doe@example.com", UserRole.CUSTOMER);
        testCustomer2 = createTestUser("Jane", "Smith", "jane.smith@example.com", UserRole.CUSTOMER);

        // Create test tables
        testTable1 = createTestTable(1, 4);
        testTable2 = createTestTable(2, 6);

        // Persist users and tables
        entityManager.persistAndFlush(testCustomer1);
        entityManager.persistAndFlush(testCustomer2);
        entityManager.persistAndFlush(testTable1);
        entityManager.persistAndFlush(testTable2);

        // Create test call requests
        testCallRequest1 = createTestCallRequest(RequestType.WATER, "Need water refill", true, testTable1, testCustomer1, LocalDateTime.now().minusHours(1));
        testCallRequest2 = createTestCallRequest(RequestType.PAYMENT, "Ready to pay", true, testTable2, testCustomer2, LocalDateTime.now().minusMinutes(30));
        testCallRequest3 = createTestCallRequest(RequestType.ASSISTANCE, "Need help with menu", false, testTable1, testCustomer1, LocalDateTime.now().minusHours(2));

        // Persist call requests
        entityManager.persistAndFlush(testCallRequest1);
        entityManager.persistAndFlush(testCallRequest2);
        entityManager.persistAndFlush(testCallRequest3);

        entityManager.clear();
    }

    private User createTestUser(String firstName, String lastName, String email, UserRole role) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setRole(role);
        user.setPassword("password123");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        user.setLoyaltyPoints(100);
        if (role != UserRole.CUSTOMER) {
            user.setSalary(BigDecimal.valueOf(3000.00));
        }
        return user;
    }

    private TableTop createTestTable(int tableNumber, int capacity) {
        TableTop table = new TableTop();
        table.setTableNumber(String.valueOf(tableNumber));
        table.setCapacity(capacity);
        table.setTableStatus(TableStatus.AVAILABLE);
        return table;
    }

    private CallRequest createTestCallRequest(RequestType type, String message, boolean active, TableTop table, User customer, LocalDateTime createdAt) {
        CallRequest callRequest = new CallRequest();
        callRequest.setType(type);
        callRequest.setMessage(message);
        callRequest.setActive(active);
        callRequest.setTable(table);
        callRequest.setCustomer(customer);
        callRequest.setCreatedAt(createdAt);
        return callRequest;
    }

    @Nested
    @DisplayName("Basic CRUD Tests")
    class BasicCrudTests {

        @Test
        @DisplayName("Should save CallRequest successfully")
        void shouldSaveCallRequest() {
            // Given
            CallRequest newCallRequest = createTestCallRequest(
                RequestType.NEED, "Need napkins", true, testTable1, testCustomer1, LocalDateTime.now()
            );

            // When
            CallRequest saved = callRequestRepository.save(newCallRequest);
            entityManager.flush();

            // Then
            assertThat(saved.getCallRequestId()).isNotNull();
            assertThat(saved.getType()).isEqualTo(RequestType.NEED);
            assertThat(saved.getMessage()).isEqualTo("Need napkins");
            assertThat(saved.isActive()).isTrue();
            assertThat(saved.getTable()).isEqualTo(testTable1);
            assertThat(saved.getCustomer()).isEqualTo(testCustomer1);
        }

        @Test
        @DisplayName("Should find CallRequest by ID")
        void shouldFindCallRequestById() {
            // When
            Optional<CallRequest> found = callRequestRepository.findById(testCallRequest1.getCallRequestId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getType()).isEqualTo(RequestType.WATER);
            assertThat(found.get().getMessage()).isEqualTo("Need water refill");
            assertThat(found.get().isActive()).isTrue();
        }

        @Test
        @DisplayName("Should update CallRequest successfully")
        void shouldUpdateCallRequest() {
            // Given
            CallRequest callRequest = callRequestRepository.findById(testCallRequest1.getCallRequestId()).orElse(null);
            assertThat(callRequest).isNotNull();

            // When
            callRequest.setActive(false);
            callRequest.setMessage("Water request fulfilled");
            CallRequest updated = callRequestRepository.save(callRequest);
            entityManager.flush();

            // Then
            assertThat(updated.isActive()).isFalse();
            assertThat(updated.getMessage()).isEqualTo("Water request fulfilled");
            assertThat(updated.getType()).isEqualTo(RequestType.WATER);
        }

        @Test
        @DisplayName("Should delete CallRequest successfully")
        void shouldDeleteCallRequest() {
            // Given
            Long callRequestId = testCallRequest1.getCallRequestId();

            // When
            callRequestRepository.deleteById(callRequestId);
            entityManager.flush();

            // Then
            Optional<CallRequest> found = callRequestRepository.findById(callRequestId);
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should find all CallRequests")
        void shouldFindAllCallRequests() {
            // When
            List<CallRequest> allCallRequests = callRequestRepository.findAll();

            // Then
            assertThat(allCallRequests).hasSize(3);
            assertThat(allCallRequests)
                .extracting(CallRequest::getType)
                .containsExactlyInAnyOrder(RequestType.WATER, RequestType.PAYMENT, RequestType.ASSISTANCE);
        }
    }

    @Nested
    @DisplayName("Custom Query Tests")
    class CustomQueryTests {

        @Test
        @DisplayName("Should find call requests by customer and created after date")
        void shouldFindByCustomerAndCreatedAtAfter() {
            // Given
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(3); // Look back 3 hours to include all test data

            // When
            List<CallRequest> recentRequests = callRequestRepository.findByCustomerAndCreatedAtAfter(testCustomer1, cutoffTime);

            // Then
            assertThat(recentRequests).hasSize(2); // Both water and assistance requests from customer1
            assertThat(recentRequests)
                .extracting(CallRequest::getType)
                .containsExactlyInAnyOrder(RequestType.WATER, RequestType.ASSISTANCE);
        }

        @Test
        @DisplayName("Should find all call requests by type with pagination")
        void shouldFindAllByTypeWithPagination() {
            // Given
            Pageable pageable = PageRequest.of(0, 2);

            // When
            Page<CallRequest> waterRequests = callRequestRepository.findAllByType(RequestType.WATER, pageable);

            // Then
            assertThat(waterRequests.getContent()).hasSize(1);
            assertThat(waterRequests.getTotalElements()).isEqualTo(1);
            assertThat(waterRequests.getContent().get(0).getType()).isEqualTo(RequestType.WATER);
        }

        @Test
        @DisplayName("Should find all active call requests with pagination")
        void shouldFindAllByActiveWithPagination() {
            // Given
            Pageable pageable = PageRequest.of(0, 5);

            // When
            Page<CallRequest> activeRequests = callRequestRepository.findAllByActive(true, pageable);

            // Then
            assertThat(activeRequests.getContent()).hasSize(2);
            assertThat(activeRequests.getTotalElements()).isEqualTo(2);
            assertThat(activeRequests.getContent())
                .allMatch(CallRequest::isActive);
        }

        @Test
        @DisplayName("Should find all call requests by type and active status with pagination")
        void shouldFindAllByTypeAndActiveWithPagination() {
            // Given
            Pageable pageable = PageRequest.of(0, 5);

            // When
            Page<CallRequest> activeWaterRequests = callRequestRepository.findAllByTypeAndActive(RequestType.WATER, true, pageable);
            Page<CallRequest> inactiveAssistanceRequests = callRequestRepository.findAllByTypeAndActive(RequestType.ASSISTANCE, false, pageable);

            // Then
            assertThat(activeWaterRequests.getContent()).hasSize(1);
            assertThat(activeWaterRequests.getContent().get(0).getType()).isEqualTo(RequestType.WATER);
            assertThat(activeWaterRequests.getContent().get(0).isActive()).isTrue();

            assertThat(inactiveAssistanceRequests.getContent()).hasSize(1);
            assertThat(inactiveAssistanceRequests.getContent().get(0).getType()).isEqualTo(RequestType.ASSISTANCE);
            assertThat(inactiveAssistanceRequests.getContent().get(0).isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("Relationship Tests")
    class RelationshipTests {

        @Test
        @DisplayName("Should maintain relationship with customer")
        void shouldMaintainRelationshipWithCustomer() {
            // When
            CallRequest callRequest = callRequestRepository.findById(testCallRequest1.getCallRequestId()).orElse(null);

            // Then
            assertThat(callRequest).isNotNull();
            assertThat(callRequest.getCustomer()).isNotNull();
            assertThat(callRequest.getCustomer().getFirstName()).isEqualTo("John");
            assertThat(callRequest.getCustomer().getEmail()).isEqualTo("john.doe@example.com");
        }

        @Test
        @DisplayName("Should maintain relationship with table")
        void shouldMaintainRelationshipWithTable() {
            // When
            CallRequest callRequest = callRequestRepository.findById(testCallRequest2.getCallRequestId()).orElse(null);

            // Then
            assertThat(callRequest).isNotNull();
            assertThat(callRequest.getTable()).isNotNull();
            assertThat(callRequest.getTable().getTableNumber()).isEqualTo("2");
            assertThat(callRequest.getTable().getCapacity()).isEqualTo(6);
        }

        @Test
        @DisplayName("Should handle call request without table")
        void shouldHandleCallRequestWithoutTable() {
            // Given
            CallRequest callRequestWithoutTable = createTestCallRequest(
                RequestType.PACK, "Take-away order ready", true, null, testCustomer1, LocalDateTime.now()
            );

            // When
            CallRequest saved = callRequestRepository.save(callRequestWithoutTable);
            entityManager.flush();

            // Then
            assertThat(saved.getCallRequestId()).isNotNull();
            assertThat(saved.getTable()).isNull();
            assertThat(saved.getCustomer()).isNotNull();
            assertThat(saved.getType()).isEqualTo(RequestType.PACK);
        }

        @Test
        @DisplayName("Should handle multiple call requests from same customer")
        void shouldHandleMultipleCallRequestsFromSameCustomer() {
            // Given
            CallRequest additionalRequest = createTestCallRequest(
                RequestType.NEED, "Need extra napkins", true, testTable1, testCustomer1, LocalDateTime.now()
            );

            // When
            callRequestRepository.save(additionalRequest);
            entityManager.flush();

            // Then
            List<CallRequest> customer1Requests = callRequestRepository.findByCustomerAndCreatedAtAfter(
                testCustomer1, LocalDateTime.now().minusHours(3)
            );
            assertThat(customer1Requests).hasSize(3); // Original 2 + new 1
        }
    }

    @Nested
    @DisplayName("Data Integrity Tests")
    class DataIntegrityTests {

        @Test
        @DisplayName("Should handle null message correctly")
        void shouldHandleNullMessageCorrectly() {
            // Given
            CallRequest callRequestWithNullMessage = createTestCallRequest(
                RequestType.ASSISTANCE, null, true, testTable1, testCustomer1, LocalDateTime.now()
            );

            // When
            CallRequest saved = callRequestRepository.save(callRequestWithNullMessage);
            entityManager.flush();

            // Then
            assertThat(saved.getCallRequestId()).isNotNull();
            assertThat(saved.getMessage()).isNull();
            assertThat(saved.getType()).isEqualTo(RequestType.ASSISTANCE);
        }

        @Test
        @DisplayName("Should default active to true")
        void shouldDefaultActiveToTrue() {
            // Given
            CallRequest callRequest = new CallRequest();
            callRequest.setType(RequestType.WATER);
            callRequest.setMessage("Test request");
            callRequest.setCustomer(testCustomer1);
            callRequest.setTable(testTable1);
            callRequest.setCreatedAt(LocalDateTime.now());
            // Not setting active explicitly

            // When
            CallRequest saved = callRequestRepository.save(callRequest);
            entityManager.flush();

            // Then
            assertThat(saved.isActive()).isTrue();
        }

        @Test
        @DisplayName("Should handle all request types correctly")
        void shouldHandleAllRequestTypesCorrectly() {
            // Given & When
            CallRequest waterRequest = callRequestRepository.save(
                createTestCallRequest(RequestType.WATER, "Water needed", true, testTable1, testCustomer1, LocalDateTime.now())
            );
            CallRequest paymentRequest = callRequestRepository.save(
                createTestCallRequest(RequestType.PAYMENT, "Ready to pay", true, testTable1, testCustomer1, LocalDateTime.now())
            );
            CallRequest assistanceRequest = callRequestRepository.save(
                createTestCallRequest(RequestType.ASSISTANCE, "Need help", true, testTable1, testCustomer1, LocalDateTime.now())
            );
            CallRequest needRequest = callRequestRepository.save(
                createTestCallRequest(RequestType.NEED, "Need something", true, testTable1, testCustomer1, LocalDateTime.now())
            );
            CallRequest packRequest = callRequestRepository.save(
                createTestCallRequest(RequestType.PACK, "Pack order", true, testTable1, testCustomer1, LocalDateTime.now())
            );
            entityManager.flush();

            // Then
            assertThat(waterRequest.getType()).isEqualTo(RequestType.WATER);
            assertThat(paymentRequest.getType()).isEqualTo(RequestType.PAYMENT);
            assertThat(assistanceRequest.getType()).isEqualTo(RequestType.ASSISTANCE);
            assertThat(needRequest.getType()).isEqualTo(RequestType.NEED);
            assertThat(packRequest.getType()).isEqualTo(RequestType.PACK);
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle empty customer request history")
        void shouldHandleEmptyCustomerRequestHistory() {
            // Given
            User newCustomer = createTestUser("Bob", "Wilson", "bob.wilson@example.com", UserRole.CUSTOMER);
            entityManager.persistAndFlush(newCustomer);

            // When
            List<CallRequest> requests = callRequestRepository.findByCustomerAndCreatedAtAfter(
                newCustomer, LocalDateTime.now().minusDays(1)
            );

            // Then
            assertThat(requests).isEmpty();
        }

        @Test
        @DisplayName("Should handle pagination with no results")
        void shouldHandlePaginationWithNoResults() {
            // Given
            Pageable pageable = PageRequest.of(0, 5);

            // When
            Page<CallRequest> packRequests = callRequestRepository.findAllByType(RequestType.PACK, pageable);

            // Then
            assertThat(packRequests.getContent()).isEmpty();
            assertThat(packRequests.getTotalElements()).isEqualTo(0);
            assertThat(packRequests.getTotalPages()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should handle very old timestamp queries")
        void shouldHandleVeryOldTimestampQueries() {
            // Given
            LocalDateTime veryOldTime = LocalDateTime.of(2020, 1, 1, 0, 0);

            // When
            List<CallRequest> requests = callRequestRepository.findByCustomerAndCreatedAtAfter(
                testCustomer1, veryOldTime
            );

            // Then
            assertThat(requests).hasSize(2); // All requests from testCustomer1
        }

        @Test
        @DisplayName("Should handle future timestamp queries")
        void shouldHandleFutureTimestampQueries() {
            // Given
            LocalDateTime futureTime = LocalDateTime.now().plusDays(1);

            // When
            List<CallRequest> requests = callRequestRepository.findByCustomerAndCreatedAtAfter(
                testCustomer1, futureTime
            );

            // Then
            assertThat(requests).isEmpty();
        }

        @Test
        @DisplayName("Should handle special characters in message")
        void shouldHandleSpecialCharactersInMessage() {
            // Given
            String specialMessage = "Need help with café & crème brûlée! @#$%^&*()";
            CallRequest specialCallRequest = createTestCallRequest(
                RequestType.ASSISTANCE, specialMessage, true, testTable1, testCustomer1, LocalDateTime.now()
            );

            // When
            CallRequest saved = callRequestRepository.save(specialCallRequest);
            entityManager.flush();
            entityManager.clear();

            CallRequest retrieved = callRequestRepository.findById(saved.getCallRequestId()).orElse(null);

            // Then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getMessage()).isEqualTo(specialMessage);
        }

        @Test
        @DisplayName("Should handle long message content within limits")
        void shouldHandleLongMessageContent() {
            // Given - Test with message close to but under 255 character limit
            String longMessage = "This is a.java reasonably long message that contains enough text to test how the database handles longer strings in the message field without exceeding the 255 character limit imposed by the database schema.";
            CallRequest longMessageRequest = createTestCallRequest(
                RequestType.ASSISTANCE, longMessage, true, testTable1, testCustomer1, LocalDateTime.now()
            );

            // When
            CallRequest saved = callRequestRepository.save(longMessageRequest);
            entityManager.flush();
            entityManager.clear();

            CallRequest retrieved = callRequestRepository.findById(saved.getCallRequestId()).orElse(null);

            // Then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getMessage()).isEqualTo(longMessage);
            assertThat(retrieved.getMessage().length()).isLessThanOrEqualTo(255);
        }
    }
}
