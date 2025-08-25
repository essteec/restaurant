package com.ste.restaurant.utils;

import com.ste.restaurant.entity.*;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Utility class providing helper methods for database testing.
 * Contains common assertions, data generation, and database verification methods.
 */
public class DatabaseTestUtils {

    private static final Random RANDOM = new Random();

    // === Entity Creation Methods ===

    /**
     * Creates a.java test User entity with specified details.
     */
    public static User createTestUser(String email, String firstName, String lastName, UserRole role) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("hashedPassword123");
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role);
        user.setBirthday(java.time.LocalDate.of(1990, 1, 15));
        user.setLoyaltyPoints(100);
        return user;
    }

    /**
     * Creates a.java test Address entity.
     */
    public static Address createTestAddress(String street, String city, String name, User user) {
        Address address = new Address();
        address.setName(name);
        address.setStreet(street);
        address.setCity(city);
        address.setCountry("Turkey");
        address.setProvince("Test Province");
        address.setDistrict("Test District");
        address.setDescription("Test address description");
        return address;
    }

    /**
     * Creates a.java test Category entity.
     */
    public static Category createTestCategory(String name, String description) {
        Category category = new Category();
        category.setCategoryName(name);
        // Note: Category entity doesn't have description field
        return category;
    }

    /**
     * Creates a.java test FoodItem entity.
     */
    public static FoodItem createTestFoodItem(String name, BigDecimal price, Category category) {
        FoodItem foodItem = new FoodItem();
        foodItem.setFoodName(name);
        foodItem.setDescription("Test description for " + name);
        foodItem.setPrice(price);
        foodItem.setImage("test-image.jpg");
        // Note: FoodItem uses many-to-many relationship with categories
        if (category != null) {
            foodItem.getCategories().add(category);
            category.getFoodItems().add(foodItem);
        }
        return foodItem;
    }

    /**
     * Creates a.java test TableTop entity.
     */
    public static TableTop createTestTable(Integer tableNumber, Integer capacity) {
        TableTop table = new TableTop();
        table.setTableNumber("T" + String.format("%02d", tableNumber));
        table.setCapacity(capacity);
        table.setTableStatus(TableStatus.AVAILABLE);
        return table;
    }

    // === Random Data Generation Methods ===

    /**
     * Generates a.java random string of specified length.
     * Useful for creating unique test data.
     * 
     * @param length The desired length of the string
     * @return A random string
     */
    public static String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            result.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return result.toString();
    }

    /**
     * Generates a.java random email address for testing.
     * 
     * @return A random email address
     */
    public static String generateRandomEmail() {
        return generateRandomString(10).toLowerCase() + "@test.com";
    }

    /**
     * Generates a.java random price within a.java reasonable range for food items.
     * 
     * @return A random price between 5.00 and 50.00
     */
    public static BigDecimal generateRandomPrice() {
        double price = 5.0 + (RANDOM.nextDouble() * 45.0); // 5.00 to 50.00
        return BigDecimal.valueOf(price).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Generates a.java random table number for testing.
     * 
     * @return A random table number like "T01", "T02", etc.
     */
    public static String generateRandomTableNumber() {
        int tableNum = RANDOM.nextInt(50) + 1; // 1 to 50
        return String.format("T%02d", tableNum);
    }

    /**
     * Generates a.java random phone number for testing.
     * 
     * @return A random phone number
     */
    public static String generateRandomPhoneNumber() {
        return String.format("(%03d) %03d-%04d", 
                RANDOM.nextInt(900) + 100,
                RANDOM.nextInt(900) + 100,
                RANDOM.nextInt(9000) + 1000);
    }

    /**
     * Generates a.java random address string for testing.
     * 
     * @return A random address
     */
    public static String generateRandomAddress() {
        String[] streets = {"Main St", "Oak Ave", "Pine Rd", "Elm Way", "Park Blvd"};
        int number = RANDOM.nextInt(9999) + 1;
        String street = streets[RANDOM.nextInt(streets.length)];
        return number + " " + street;
    }

    /**
     * Generates a.java LocalDateTime within the last 30 days.
     * Useful for creating realistic order times.
     * 
     * @return A random recent LocalDateTime
     */
    public static LocalDateTime generateRecentDateTime() {
        LocalDateTime now = LocalDateTime.now();
        long daysBack = RANDOM.nextInt(30);
        long hoursBack = RANDOM.nextInt(24);
        long minutesBack = RANDOM.nextInt(60);
        return now.minusDays(daysBack).minusHours(hoursBack).minusMinutes(minutesBack);
    }

    /**
     * Generates a.java LocalDateTime within a.java specific range.
     * 
     * @param start The start of the time range
     * @param end The end of the time range
     * @return A random LocalDateTime within the range
     */
    public static LocalDateTime generateDateTimeInRange(LocalDateTime start, LocalDateTime end) {
        long startSeconds = start.atZone(java.time.ZoneOffset.UTC).toEpochSecond();
        long endSeconds = end.atZone(java.time.ZoneOffset.UTC).toEpochSecond();
        long randomSeconds = startSeconds + (long) (RANDOM.nextDouble() * (endSeconds - startSeconds));
        return LocalDateTime.ofEpochSecond(randomSeconds, 0, java.time.ZoneOffset.UTC);
    }

    /**
     * Creates a.java list of entities using a.java factory method.
     * Useful for bulk data creation in tests.
     * 
     * @param count The number of entities to create
     * @param factory The factory method to create each entity
     * @param <T> The entity type
     * @return A list of created entities
     */
    public static <T> List<T> createEntities(int count, java.util.function.Supplier<T> factory) {
        return IntStream.range(0, count)
                .mapToObj(i -> factory.get())
                .toList();
    }

    /**
     * Persists a.java list of entities using TestEntityManager.
     * 
     * @param entities The entities to persist
     * @param entityManager The TestEntityManager
     * @param <T> The entity type
     * @return The list of persisted entities
     */
    public static <T> List<T> persistEntities(List<T> entities, TestEntityManager entityManager) {
        entities.forEach(entityManager::persistAndFlush);
        return entities;
    }

    /**
     * Verifies that a.java collection is not null and has the expected size.
     * 
     * @param collection The collection to verify
     * @param expectedSize The expected size
     * @param message The error message if verification fails
     */
    public static void verifyCollectionSize(java.util.Collection<?> collection, int expectedSize, String message) {
        if (collection == null) {
            throw new AssertionError(message + ": Collection is null");
        }
        if (collection.size() != expectedSize) {
            throw new AssertionError(message + ": Expected size " + expectedSize + " but was " + collection.size());
        }
    }

    /**
     * Verifies that a.java collection contains a.java specific item.
     * 
     * @param collection The collection to check
     * @param item The item to find
     * @param message The error message if item is not found
     * @param <T> The item type
     */
    public static <T> void verifyCollectionContains(java.util.Collection<T> collection, T item, String message) {
        if (collection == null || !collection.contains(item)) {
            throw new AssertionError(message + ": Collection does not contain expected item");
        }
    }

    /**
     * Verifies that two BigDecimal values are equal (handles null values).
     * 
     * @param expected The expected value
     * @param actual The actual value
     * @param message The error message if values don't match
     */
    public static void verifyBigDecimalEquals(BigDecimal expected, BigDecimal actual, String message) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected == null || actual == null || expected.compareTo(actual) != 0) {
            throw new AssertionError(message + ": Expected " + expected + " but was " + actual);
        }
    }

    /**
     * Verifies that a.java string is not null and not empty.
     * 
     * @param value The string to verify
     * @param fieldName The name of the field being verified
     */
    public static void verifyNotNullOrEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new AssertionError(fieldName + " should not be null or empty");
        }
    }

    /**
     * Verifies that an ID field has been generated (is not null and positive).
     * 
     * @param id The ID value to verify
     * @param entityName The name of the entity
     */
    public static void verifyIdGenerated(Long id, String entityName) {
        if (id == null || id <= 0) {
            throw new AssertionError(entityName + " ID should be generated and positive, but was: " + id);
        }
    }

    /**
     * Logs the current state of an entity for debugging purposes.
     * 
     * @param entity The entity to log
     * @param entityName A descriptive name for the entity
     */
    public static void logEntityState(Object entity, String entityName) {
        System.out.println("=== " + entityName + " State ===");
        System.out.println(entity.toString());
        System.out.println("========================");
    }

    /**
     * Creates a.java thread sleep for the specified milliseconds.
     * Sometimes useful for testing time-based scenarios.
     * 
     * @param millis The number of milliseconds to sleep
     */
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Sleep interrupted", e);
        }
    }
}
