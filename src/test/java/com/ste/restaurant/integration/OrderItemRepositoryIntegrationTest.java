package com.ste.restaurant.integration;

import com.ste.restaurant.entity.*;
import com.ste.restaurant.repository.OrderItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("OrderItemRepository Integration Tests")
class OrderItemRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrderItemRepository orderItemRepository;

    private Category testCategory;
    private Menu testMenu;
    private FoodItem testFoodItem;
    private User testCustomer;
    private Address testAddress;
    private TableTop testTable;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        // Create and persist test entities
        testCategory = new Category();
        testCategory.setCategoryName("Test Category");
        testCategory = entityManager.persistAndFlush(testCategory);

        testMenu = new Menu();
        testMenu.setMenuName("Test Menu");
        testMenu = entityManager.persistAndFlush(testMenu);

        testFoodItem = new FoodItem();
        testFoodItem.setFoodName("Test Food");
        testFoodItem.setDescription("Test food description");
        testFoodItem.setPrice(new BigDecimal("25.50"));
        testFoodItem.setImage("/images/test.jpg");
        testFoodItem = entityManager.persistAndFlush(testFoodItem);

        testCustomer = new User();
        testCustomer.setFirstName("Test");
        testCustomer.setLastName("Customer");
        testCustomer.setEmail("test@customer.com");
        testCustomer.setRole(UserRole.CUSTOMER);
        testCustomer = entityManager.persistAndFlush(testCustomer);

        testAddress = new Address();
        testAddress.setName("Test Address");
        testAddress.setCountry("Turkey");
        testAddress.setCity("Istanbul");
        testAddress.setProvince("Istanbul");
        testAddress.setDistrict("Besiktas");
        testAddress.setStreet("Test Street");
        testAddress = entityManager.persistAndFlush(testAddress);

        testTable = new TableTop();
        testTable.setTableNumber("1");
        testTable.setCapacity(4);
        testTable.setTableStatus(TableStatus.AVAILABLE);
        testTable = entityManager.persistAndFlush(testTable);

        testOrder = new Order();
        testOrder.setOrderTime(LocalDateTime.now());
        testOrder.setStatus(OrderStatus.PLACED);
        testOrder.setTotalPrice(new BigDecimal("50.00"));
        testOrder.setCustomer(testCustomer);
        testOrder.setAddress(testAddress);
        testOrder.setTable(testTable);
        testOrder = entityManager.persistAndFlush(testOrder);

        entityManager.clear();
    }

    private OrderItem createTestOrderItem(int quantity, String note) {
        OrderItem orderItem = new OrderItem();
        orderItem.setQuantity(quantity);
        orderItem.setUnitPrice(testFoodItem.getPrice());
        orderItem.setTotalPrice(testFoodItem.getPrice().multiply(BigDecimal.valueOf(quantity)));
        orderItem.setNote(note);
        orderItem.setFoodItem(testFoodItem);
        orderItem.setOrder(testOrder);
        return orderItem;
    }

    @Nested
    @DisplayName("Basic CRUD Operations")
    class BasicCrudTests {

        @Test
        @DisplayName("Should save and retrieve order item successfully")
        void shouldSaveAndRetrieveOrderItem() {
            // Given
            OrderItem orderItem = createTestOrderItem(2, "Extra spicy");

            // When
            OrderItem saved = orderItemRepository.save(orderItem);
            entityManager.flush();
            entityManager.clear();

            Optional<OrderItem> retrieved = orderItemRepository.findById(saved.getOrderItemId());

            // Then
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getQuantity()).isEqualTo(2);
            assertThat(retrieved.get().getUnitPrice()).isEqualTo(new BigDecimal("25.50"));
            assertThat(retrieved.get().getTotalPrice()).isEqualTo(new BigDecimal("51.00"));
            assertThat(retrieved.get().getNote()).isEqualTo("Extra spicy");
            assertThat(retrieved.get().getFoodItem().getFoodName()).isEqualTo("Test Food");
            assertThat(retrieved.get().getOrder().getOrderId()).isEqualTo(testOrder.getOrderId());
        }

        @Test
        @DisplayName("Should update order item successfully")
        void shouldUpdateOrderItem() {
            // Given
            OrderItem orderItem = createTestOrderItem(1, "Original note");
            OrderItem saved = orderItemRepository.save(orderItem);
            entityManager.flush();
            entityManager.clear();

            // When
            saved.setQuantity(3);
            saved.setTotalPrice(testFoodItem.getPrice().multiply(BigDecimal.valueOf(3)));
            saved.setNote("Updated note");
            OrderItem updated = orderItemRepository.save(saved);
            entityManager.flush();
            entityManager.clear();

            OrderItem retrieved = orderItemRepository.findById(updated.getOrderItemId()).orElse(null);

            // Then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getQuantity()).isEqualTo(3);
            assertThat(retrieved.getTotalPrice()).isEqualTo(new BigDecimal("76.50"));
            assertThat(retrieved.getNote()).isEqualTo("Updated note");
        }

        @Test
        @DisplayName("Should delete order item by ID successfully")
        void shouldDeleteOrderItemById() {
            // Given
            OrderItem orderItem = createTestOrderItem(1, "Test note");
            OrderItem saved = orderItemRepository.save(orderItem);
            entityManager.flush();
            Long orderItemId = saved.getOrderItemId();

            // When
            orderItemRepository.deleteById(orderItemId);
            entityManager.flush();

            // Then
            Optional<OrderItem> retrieved = orderItemRepository.findById(orderItemId);
            assertThat(retrieved).isEmpty();
        }

        @Test
        @DisplayName("Should delete order item by entity successfully")
        void shouldDeleteOrderItemByEntity() {
            // Given
            OrderItem orderItem = createTestOrderItem(1, "Test note");
            OrderItem saved = orderItemRepository.save(orderItem);
            entityManager.flush();
            Long orderItemId = saved.getOrderItemId();

            // When
            orderItemRepository.delete(saved);
            entityManager.flush();

            // Then
            Optional<OrderItem> retrieved = orderItemRepository.findById(orderItemId);
            assertThat(retrieved).isEmpty();
        }

        @Test
        @DisplayName("Should return all order items")
        void shouldReturnAllOrderItems() {
            // Given
            orderItemRepository.saveAll(List.of(
                createTestOrderItem(1, "Item 1"),
                createTestOrderItem(2, "Item 2"),
                createTestOrderItem(3, "Item 3")
            ));
            entityManager.flush();

            // When
            List<OrderItem> orderItems = orderItemRepository.findAll();

            // Then
            assertThat(orderItems).hasSize(3);
            assertThat(orderItems.stream().map(OrderItem::getNote))
                .containsExactlyInAnyOrder("Item 1", "Item 2", "Item 3");
        }
    }

    @Nested
    @DisplayName("Relationship Tests")
    class RelationshipTests {

        @Test
        @DisplayName("Should maintain relationship with FoodItem")
        void shouldMaintainRelationshipWithFoodItem() {
            // Given
            OrderItem orderItem = createTestOrderItem(2, "Test relationship");

            // When
            OrderItem saved = orderItemRepository.save(orderItem);
            entityManager.flush();
            entityManager.clear();

            OrderItem retrieved = orderItemRepository.findById(saved.getOrderItemId()).orElse(null);

            // Then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getFoodItem()).isNotNull();
            assertThat(retrieved.getFoodItem().getFoodName()).isEqualTo("Test Food");
            assertThat(retrieved.getFoodItem().getPrice()).isEqualTo(new BigDecimal("25.50"));
        }

        @Test
        @DisplayName("Should maintain relationship with Order")
        void shouldMaintainRelationshipWithOrder() {
            // Given
            OrderItem orderItem = createTestOrderItem(1, "Test order relationship");

            // When
            OrderItem saved = orderItemRepository.save(orderItem);
            entityManager.flush();
            entityManager.clear();

            OrderItem retrieved = orderItemRepository.findById(saved.getOrderItemId()).orElse(null);

            // Then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getOrder()).isNotNull();
            assertThat(retrieved.getOrder().getOrderId()).isEqualTo(testOrder.getOrderId());
            assertThat(retrieved.getOrder().getStatus()).isEqualTo(OrderStatus.PLACED);
            assertThat(retrieved.getOrder().getCustomer().getEmail()).isEqualTo("test@customer.com");
        }

        @Test
        @DisplayName("Should handle multiple order items for same order")
        void shouldHandleMultipleOrderItemsForSameOrder() {
            // Given
            OrderItem item1 = createTestOrderItem(1, "First item");
            OrderItem item2 = createTestOrderItem(2, "Second item");
            OrderItem item3 = createTestOrderItem(3, "Third item");

            // When
            List<OrderItem> saved = orderItemRepository.saveAll(List.of(item1, item2, item3));
            entityManager.flush();
            entityManager.clear();

            List<OrderItem> retrieved = orderItemRepository.findAllById(
                saved.stream().map(OrderItem::getOrderItemId).toList()
            );

            // Then
            assertThat(retrieved).hasSize(3);
            assertThat(retrieved.stream().map(item -> item.getOrder().getOrderId()))
                .allMatch(orderId -> orderId.equals(testOrder.getOrderId()));
        }
    }

    @Nested
    @DisplayName("Data Integrity Tests")
    class DataIntegrityTests {

        @Test
        @DisplayName("Should handle null note field")
        void shouldHandleNullNoteField() {
            // Given
            OrderItem orderItem = createTestOrderItem(1, null);

            // When
            OrderItem saved = orderItemRepository.save(orderItem);
            entityManager.flush();
            entityManager.clear();

            OrderItem retrieved = orderItemRepository.findById(saved.getOrderItemId()).orElse(null);

            // Then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getNote()).isNull();
            assertThat(retrieved.getQuantity()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should handle empty note field")
        void shouldHandleEmptyNoteField() {
            // Given
            OrderItem orderItem = createTestOrderItem(1, "");

            // When
            OrderItem saved = orderItemRepository.save(orderItem);
            entityManager.flush();
            entityManager.clear();

            OrderItem retrieved = orderItemRepository.findById(saved.getOrderItemId()).orElse(null);

            // Then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getNote()).isEmpty();
        }

        @Test
        @DisplayName("Should handle zero quantity")
        void shouldHandleZeroQuantity() {
            // Given
            OrderItem orderItem = createTestOrderItem(0, "Zero quantity item");

            // When
            OrderItem saved = orderItemRepository.save(orderItem);
            entityManager.flush();
            entityManager.clear();

            OrderItem retrieved = orderItemRepository.findById(saved.getOrderItemId()).orElse(null);

            // Then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getQuantity()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle large quantities")
        void shouldHandleLargeQuantities() {
            // Given
            OrderItem orderItem = createTestOrderItem(999, "Large quantity");

            // When
            OrderItem saved = orderItemRepository.save(orderItem);
            entityManager.flush();
            entityManager.clear();

            OrderItem retrieved = orderItemRepository.findById(saved.getOrderItemId()).orElse(null);

            // Then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getQuantity()).isEqualTo(999);
            assertThat(retrieved.getTotalPrice()).isEqualTo(new BigDecimal("25474.50"));
        }

        @Test
        @DisplayName("Should handle negative quantities")
        void shouldHandleNegativeQuantities() {
            // Given
            OrderItem orderItem = createTestOrderItem(-1, "Negative quantity");

            // When
            OrderItem saved = orderItemRepository.save(orderItem);
            entityManager.flush();
            entityManager.clear();

            OrderItem retrieved = orderItemRepository.findById(saved.getOrderItemId()).orElse(null);

            // Then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getQuantity()).isEqualTo(-1);
            assertThat(retrieved.getTotalPrice()).isEqualTo(new BigDecimal("-25.50"));
        }

        @Test
        @DisplayName("Should handle long notes")
        void shouldHandleLongNotes() {
            // Given - Create a note that's long but within database limits
            String longNote = "This is a detailed note that describes special preparation instructions. ".repeat(3);
            OrderItem orderItem = createTestOrderItem(1, longNote);

            // When
            OrderItem saved = orderItemRepository.save(orderItem);
            entityManager.flush();
            entityManager.clear();

            OrderItem retrieved = orderItemRepository.findById(saved.getOrderItemId()).orElse(null);

            // Then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getNote()).isEqualTo(longNote);
            assertThat(retrieved.getNote().length()).isLessThanOrEqualTo(255);
        }

        @Test
        @DisplayName("Should handle special characters in notes")
        void shouldHandleSpecialCharactersInNotes() {
            // Given
            String specialNote = "Özel not: çok baharatlı & acı! 🌶️ (ekstra sos istiyorum)";
            OrderItem orderItem = createTestOrderItem(1, specialNote);

            // When
            OrderItem saved = orderItemRepository.save(orderItem);
            entityManager.flush();
            entityManager.clear();

            OrderItem retrieved = orderItemRepository.findById(saved.getOrderItemId()).orElse(null);

            // Then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getNote()).isEqualTo(specialNote);
        }

        @Test
        @DisplayName("Should handle precision in BigDecimal calculations")
        void shouldHandlePrecisionInBigDecimalCalculations() {
            // Given - Use scale that matches database constraints (typically 2 decimal places)
            OrderItem orderItem = new OrderItem();
            orderItem.setQuantity(3);
            orderItem.setUnitPrice(new BigDecimal("10.33"));
            orderItem.setTotalPrice(orderItem.getUnitPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity())));
            orderItem.setNote("Precision test");
            orderItem.setFoodItem(testFoodItem);
            orderItem.setOrder(testOrder);

            // When
            OrderItem saved = orderItemRepository.save(orderItem);
            entityManager.flush();
            entityManager.clear();

            OrderItem retrieved = orderItemRepository.findById(saved.getOrderItemId()).orElse(null);

            // Then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getUnitPrice()).isEqualTo(new BigDecimal("10.33"));
            assertThat(retrieved.getTotalPrice()).isEqualTo(new BigDecimal("30.99"));
        }

        @Test
        @DisplayName("Should handle concurrent modifications")
        void shouldHandleConcurrentModifications() {
            // Given
            OrderItem orderItem = createTestOrderItem(1, "Concurrent test");
            OrderItem saved = orderItemRepository.save(orderItem);
            entityManager.flush();
            entityManager.clear();

            // When - Simulate concurrent modifications
            OrderItem instance1 = orderItemRepository.findById(saved.getOrderItemId()).orElse(null);
            OrderItem instance2 = orderItemRepository.findById(saved.getOrderItemId()).orElse(null);

            instance1.setQuantity(2);
            instance2.setNote("Modified note");

            orderItemRepository.save(instance1);
            entityManager.flush();
            OrderItem finalSaved = orderItemRepository.save(instance2);
            entityManager.flush();

            // Then
            assertThat(finalSaved.getQuantity()).isEqualTo(2);
            assertThat(finalSaved.getNote()).isEqualTo("Modified note");
        }
    }
}
