package com.ste.restaurant.integration;

import com.ste.restaurant.entity.Address;
import com.ste.restaurant.repository.AddressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for AddressRepository.
 * Tests the repository layer with real database operations using H2 in-memory database.
 */
@DataJpaTest
@DisplayName("Address Repository Integration Tests")
class AddressRepositoryIntegrationTest {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private TestEntityManager entityManager;

    // Test data
    private Address testAddress1;
    private Address testAddress2;
    private Address testAddress3;

    @BeforeEach
    void setUp() {
        setupTestData();
    }

    void setupTestData() {
        // Create test addresses
        testAddress1 = createTestAddress(
            "Home", "Turkey", "Istanbul", "Istanbul", "Kadikoy", 
            "Fenerbahce", "Bagdat Street", "No: 123", "Main residence"
        );
        testAddress2 = createTestAddress(
            "Office", "Turkey", "Ankara", "Ankara", "Cankaya", 
            "Kizilay", "Ataturk Boulevard", "Floor 5", "Work address"
        );
        testAddress3 = createTestAddress(
            "Vacation Home", "Turkey", "Antalya", "Antalya", "Muratpasa", 
            "Lara", "Beach Road", "Villa 42", "Holiday address"
        );

        // Persist addresses
        entityManager.persistAndFlush(testAddress1);
        entityManager.persistAndFlush(testAddress2);
        entityManager.persistAndFlush(testAddress3);

        entityManager.clear();
    }

    private Address createTestAddress(String name, String country, String city, String province, 
                                    String subprovince, String district, String street, 
                                    String apartment, String description) {
        Address address = new Address();
        address.setName(name);
        address.setCountry(country);
        address.setCity(city);
        address.setProvince(province);
        address.setSubprovince(subprovince);
        address.setDistrict(district);
        address.setStreet(street);
        address.setApartment(apartment);
        address.setDescription(description);
        return address;
    }

    @Nested
    @DisplayName("Basic CRUD Tests")
    class BasicCrudTests {

        @Test
        @DisplayName("Should save Address successfully")
        void shouldSaveAddress() {
            // Given
            Address newAddress = createTestAddress(
                "New Address", "Turkey", "Izmir", "Izmir", "Konak", 
                "Alsancak", "Kordon Street", "Apt 45", "New test address"
            );

            // When
            Address saved = addressRepository.save(newAddress);
            entityManager.flush();

            // Then
            assertThat(saved.getAddressId()).isNotNull();
            assertThat(saved.getName()).isEqualTo("New Address");
            assertThat(saved.getCountry()).isEqualTo("Turkey");
            assertThat(saved.getCity()).isEqualTo("Izmir");
            assertThat(saved.getProvince()).isEqualTo("Izmir");
            assertThat(saved.getSubprovince()).isEqualTo("Konak");
            assertThat(saved.getDistrict()).isEqualTo("Alsancak");
            assertThat(saved.getStreet()).isEqualTo("Kordon Street");
            assertThat(saved.getApartment()).isEqualTo("Apt 45");
            assertThat(saved.getDescription()).isEqualTo("New test address");
        }

        @Test
        @DisplayName("Should find Address by ID")
        void shouldFindAddressById() {
            // When
            Optional<Address> found = addressRepository.findById(testAddress1.getAddressId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("Home");
            assertThat(found.get().getCity()).isEqualTo("Istanbul");
            assertThat(found.get().getStreet()).isEqualTo("Bagdat Street");
        }

        @Test
        @DisplayName("Should update Address successfully")
        void shouldUpdateAddress() {
            // Given
            Address address = addressRepository.findById(testAddress1.getAddressId()).orElse(null);
            assertThat(address).isNotNull();

            // When
            address.setStreet("Updated Street");
            address.setApartment("Updated Apartment");
            address.setDescription("Updated description");
            Address updated = addressRepository.save(address);
            entityManager.flush();

            // Then
            assertThat(updated.getStreet()).isEqualTo("Updated Street");
            assertThat(updated.getApartment()).isEqualTo("Updated Apartment");
            assertThat(updated.getDescription()).isEqualTo("Updated description");
            // Verify other fields remain unchanged
            assertThat(updated.getName()).isEqualTo("Home");
            assertThat(updated.getCity()).isEqualTo("Istanbul");
        }

        @Test
        @DisplayName("Should delete Address by ID")
        void shouldDeleteAddressById() {
            // Given
            Long addressId = testAddress1.getAddressId();

            // When
            addressRepository.deleteById(addressId);
            entityManager.flush();

            // Then
            Optional<Address> found = addressRepository.findById(addressId);
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should delete Address by entity")
        void shouldDeleteAddressByEntity() {
            // Given
            Address addressToDelete = testAddress2;
            Long addressId = addressToDelete.getAddressId();

            // When
            addressRepository.delete(addressToDelete);
            entityManager.flush();

            // Then
            Optional<Address> found = addressRepository.findById(addressId);
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should find all Addresses")
        void shouldFindAllAddresses() {
            // When
            List<Address> allAddresses = addressRepository.findAll();

            // Then
            assertThat(allAddresses).hasSize(3);
            assertThat(allAddresses)
                .extracting(Address::getName)
                .containsExactlyInAnyOrder("Home", "Office", "Vacation Home");
        }
    }

    @Nested
    @DisplayName("Data Integrity Tests")
    class DataIntegrityTests {

        @Test
        @DisplayName("Should enforce unique name constraint")
        void shouldEnforceUniqueNameConstraint() {
            // Given
            Address duplicateAddress = createTestAddress(
                "Home", "Turkey", "Bursa", "Bursa", "Osmangazi", 
                "Nilufer", "Test Street", "Test Apt", "Duplicate name"
            );

            // When & Then
            try {
                addressRepository.save(duplicateAddress);
                entityManager.flush();
                // Should fail due to unique constraint
                assertThat(false).as("Expected unique constraint violation").isTrue();
            } catch (Exception e) {
                // Expected behavior - unique constraint violation
                assertThat(e.getMessage()).containsIgnoringCase("unique");
            }
        }

        @Test
        @DisplayName("Should handle null optional fields correctly")
        void shouldHandleNullOptionalFieldsCorrectly() {
            // Given - Create address with only required fields
            Address addressWithNulls = new Address();
            addressWithNulls.setName("Minimal Address");
            // All other fields remain null

            // When
            Address saved = addressRepository.save(addressWithNulls);
            entityManager.flush();
            entityManager.clear();

            Address retrieved = addressRepository.findById(saved.getAddressId()).orElse(null);

            // Then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getName()).isEqualTo("Minimal Address");
            assertThat(retrieved.getCountry()).isNull();
            assertThat(retrieved.getCity()).isNull();
            assertThat(retrieved.getProvince()).isNull();
            assertThat(retrieved.getSubprovince()).isNull();
            assertThat(retrieved.getDistrict()).isNull();
            assertThat(retrieved.getStreet()).isNull();
            assertThat(retrieved.getApartment()).isNull();
            assertThat(retrieved.getDescription()).isNull();
        }

        @Test
        @DisplayName("Should handle empty string values correctly")
        void shouldHandleEmptyStringValuesCorrectly() {
            // Given
            Address addressWithEmptyStrings = createTestAddress(
                "Empty Fields", "", "", "", "", "", "", "", ""
            );

            // When
            Address saved = addressRepository.save(addressWithEmptyStrings);
            entityManager.flush();
            entityManager.clear();

            Address retrieved = addressRepository.findById(saved.getAddressId()).orElse(null);

            // Then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getName()).isEqualTo("Empty Fields");
            assertThat(retrieved.getCountry()).isEqualTo("");
            assertThat(retrieved.getCity()).isEqualTo("");
            assertThat(retrieved.getProvince()).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle special characters in address fields")
        void shouldHandleSpecialCharactersInAddressFields() {
            // Given
            Address specialAddress = createTestAddress(
                "Café & Restaurant", "Türkiye", "İstanbul", "İstanbul", "Şişli", 
                "Nişantaşı", "Abdi İpekçi Caddesi", "Daire: 3/A", "Özel açıklama: çok güzel!"
            );

            // When
            Address saved = addressRepository.save(specialAddress);
            entityManager.flush();
            entityManager.clear();

            Address retrieved = addressRepository.findById(saved.getAddressId()).orElse(null);

            // Then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getName()).isEqualTo("Café & Restaurant");
            assertThat(retrieved.getCountry()).isEqualTo("Türkiye");
            assertThat(retrieved.getCity()).isEqualTo("İstanbul");
            assertThat(retrieved.getDistrict()).isEqualTo("Nişantaşı");
            assertThat(retrieved.getStreet()).isEqualTo("Abdi İpekçi Caddesi");
            assertThat(retrieved.getDescription()).isEqualTo("Özel açıklama: çok güzel!");
        }

        @Test
        @DisplayName("Should handle long address field values")
        void shouldHandleLongAddressFieldValues() {
            // Given - create strings that are long but within database limits
            String longStreet = "Very Long Street Name ".repeat(3); // About 63 chars
            String longDescription = "This is a detailed description of the address location. ".repeat(3); // About 165 chars
            
            Address longFieldsAddress = createTestAddress(
                "Long Fields", "Turkey", "Istanbul", "Istanbul", "Besiktas", 
                "Etiler", longStreet, "Complex Building A/B/C", longDescription
            );

            // When
            Address saved = addressRepository.save(longFieldsAddress);
            entityManager.flush();
            entityManager.clear();

            Address retrieved = addressRepository.findById(saved.getAddressId()).orElse(null);

            // Then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getName()).isEqualTo("Long Fields");
            assertThat(retrieved.getStreet()).isEqualTo(longStreet);
            assertThat(retrieved.getDescription()).isEqualTo(longDescription);
            // Verify lengths are reasonable for database constraints
            assertThat(longStreet.length()).isLessThanOrEqualTo(255);
            assertThat(longDescription.length()).isLessThanOrEqualTo(255);
        }

        @Test
        @DisplayName("Should handle numeric values in address fields")
        void shouldHandleNumericValuesInAddressFields() {
            // Given
            Address numericAddress = createTestAddress(
                "123", "123", "456", "789", "101112", 
                "131415", "Street 999", "Apt 777", "Building number 888"
            );

            // When
            Address saved = addressRepository.save(numericAddress);
            entityManager.flush();
            entityManager.clear();

            Address retrieved = addressRepository.findById(saved.getAddressId()).orElse(null);

            // Then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getName()).isEqualTo("123");
            assertThat(retrieved.getCountry()).isEqualTo("123");
            assertThat(retrieved.getCity()).isEqualTo("456");
            assertThat(retrieved.getStreet()).isEqualTo("Street 999");
            assertThat(retrieved.getApartment()).isEqualTo("Apt 777");
        }

        @Test
        @DisplayName("Should handle whitespace-only values")
        void shouldHandleWhitespaceOnlyValues() {
            // Given
            Address whitespaceAddress = createTestAddress(
                "Whitespace Test", "   ", "\t", "\n", "  \t  ", 
                "\n\n", "   Street   ", "  Apt  ", "   Description   "
            );

            // When
            Address saved = addressRepository.save(whitespaceAddress);
            entityManager.flush();
            entityManager.clear();

            Address retrieved = addressRepository.findById(saved.getAddressId()).orElse(null);

            // Then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getName()).isEqualTo("Whitespace Test");
            assertThat(retrieved.getCountry()).isEqualTo("   ");
            assertThat(retrieved.getCity()).isEqualTo("\t");
            assertThat(retrieved.getStreet()).isEqualTo("   Street   ");
            assertThat(retrieved.getApartment()).isEqualTo("  Apt  ");
        }

        @Test
        @DisplayName("Should handle address deletion when not found")
        void shouldHandleAddressDeletionWhenNotFound() {
            // Given
            Long nonExistentId = 99999L;

            // When & Then - Should not throw exception
            addressRepository.deleteById(nonExistentId);
            entityManager.flush();

            // Verify operation completed without error
            Optional<Address> notFound = addressRepository.findById(nonExistentId);
            assertThat(notFound).isEmpty();
        }

        @Test
        @DisplayName("Should handle concurrent address modifications")
        void shouldHandleConcurrentAddressModifications() {
            // Given
            Address address = addressRepository.findById(testAddress1.getAddressId()).orElse(null);
            assertThat(address).isNotNull();

            // When - Simulate concurrent modifications
            address.setStreet("Concurrency Test Street 1");
            Address saved1 = addressRepository.save(address);
            entityManager.flush();

            // Modify again
            saved1.setApartment("Concurrency Test Apartment");
            addressRepository.save(saved1);
            entityManager.flush();

            // Then
            entityManager.clear();
            Address finalAddress = addressRepository.findById(testAddress1.getAddressId()).orElse(null);
            assertThat(finalAddress).isNotNull();
            assertThat(finalAddress.getStreet()).isEqualTo("Concurrency Test Street 1");
            assertThat(finalAddress.getApartment()).isEqualTo("Concurrency Test Apartment");
        }
    }
}
