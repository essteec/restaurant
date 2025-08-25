package com.ste.restaurant.service;

import com.ste.restaurant.dto.AddressDto;
import com.ste.restaurant.entity.Address;
import com.ste.restaurant.entity.User;
import com.ste.restaurant.exception.NotFoundException;
import com.ste.restaurant.exception.NullValueException;
import com.ste.restaurant.mapper.OrderMapper;
import com.ste.restaurant.repository.AddressRepository;
import com.ste.restaurant.repository.OrderRepository;
import com.ste.restaurant.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private AddressService addressService;

    private User testUser;
    private Address testAddress1;
    private Address testAddress2;
    private AddressDto testAddressDto1;
    private AddressDto testAddressDto2;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setEmail("test@example.com");
        testUser.setAddresses(new ArrayList<>()); // Initialize an addresses list

        testAddress1 = new Address();
        testAddress1.setAddressId(101L);
        testAddress1.setName("Home Address");

        testAddress2 = new Address();
        testAddress2.setAddressId(102L);
        testAddress2.setName("Work Address");

        testAddressDto1 = new AddressDto(101L, "Home Address", "Country", "City", "Province", null, null, "Street", "Apartment", null);
        testAddressDto2 = new AddressDto(102L, "Work Address", "Country", "City", "Province", null, null, "Street", "Apartment", null);
    }

    @Test
    void getAddressesOfUser_success() {
        // Arrange
        Long userId = 1L;
        testUser.getAddresses().add(testAddress1);
        testUser.getAddresses().add(testAddress2);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser)); // Added mock for findByEmail
        when(orderMapper.addressesToAddressDtos(testUser.getAddresses()))
                .thenReturn(Arrays.asList(testAddressDto1, testAddressDto2));

        // Act
        List<AddressDto> result = addressService.getAddressesOfUser(userId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(testAddressDto1.getAddressId(), result.get(0).getAddressId());
        assertEquals(testAddressDto2.getAddressId(), result.get(1).getAddressId());

        // Verify
        verify(userRepository, times(1)).findById(userId);
        verify(orderMapper, times(1)).addressesToAddressDtos(testUser.getAddresses());
    }

    @Test
    void getAddressesOfUser_userNotFound() {
        // Arrange
        Long userId = 99L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException thrown = assertThrows(NotFoundException.class, () -> addressService.getAddressesOfUser(userId));

        assertEquals("User not found", thrown.getMessage()); // Corrected assertion

        // Verify
        verify(userRepository, times(1)).findById(userId);
        verify(orderMapper, never()).addressesToAddressDtos(anyList());
    }

    @Test
    void saveAddress_success() {
        // Arrange
        String userEmail = "test@example.com";
        AddressDto inputAddressDto = new AddressDto(null, "New Address", "Country", "City", "Province", null, null, "Street", "Apartment", null);
        Address addressToSave = new Address();
        addressToSave.setName("New Address");
        Address savedAddress = new Address();
        savedAddress.setAddressId(1L);
        savedAddress.setName("New Address");

        // Create a.java DTO that matches the savedAddress for the return value mock
        AddressDto savedAddressDto = new AddressDto(savedAddress.getAddressId(), savedAddress.getName(), "Country", "City", "Province", null, null, "Street", "Apartment", null);

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(orderMapper.addressDtoToAddress(inputAddressDto)).thenReturn(addressToSave);
        when(addressRepository.save(addressToSave)).thenReturn(savedAddress);
        when(userRepository.save(testUser)).thenReturn(testUser); // Simulate saving user with new address
        when(orderMapper.addressToAddressDto(savedAddress)).thenReturn(savedAddressDto); // Use the DTO that matches savedAddress

        // Act
        AddressDto result = addressService.saveAddress(inputAddressDto, userEmail);

        // Assert
        assertNotNull(result);
        assertEquals(savedAddressDto.getAddressId(), result.getAddressId());
        assertEquals(savedAddressDto.getName(), result.getName()); // Assert against the name from savedAddressDto
        assertTrue(testUser.getAddresses().contains(savedAddress)); // Verify address was added to the user

        // Verify
        verify(userRepository, times(1)).findByEmail(userEmail);
        verify(orderMapper, times(1)).addressDtoToAddress(inputAddressDto);
        verify(addressRepository, times(1)).save(addressToSave);
        verify(userRepository, times(1)).save(testUser);
        verify(orderMapper, times(1)).addressToAddressDto(savedAddress);
    }

    @Test
    void saveAddress_userNotFound() {
        // Arrange
        String userEmail = "nonexistent@example.com";
        AddressDto inputAddressDto = new AddressDto(null, "New Address", "Country", "City", "Province", null, null, "Street", "Apartment", null);

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException thrown = assertThrows(NotFoundException.class, () -> addressService.saveAddress(inputAddressDto, userEmail));

        assertEquals("User not found", thrown.getMessage()); // Corrected assertion

        // Verify
        verify(userRepository, times(1)).findByEmail(userEmail);
        verify(orderMapper, never()).addressDtoToAddress(any(AddressDto.class));
        verify(addressRepository, never()).save(any(Address.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getAddresses_success() {
        // Arrange
        String userEmail = testUser.getEmail();
        testUser.getAddresses().add(testAddress1);
        testUser.getAddresses().add(testAddress2);

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(orderMapper.addressesToAddressDtos(testUser.getAddresses()))
                .thenReturn(Arrays.asList(testAddressDto1, testAddressDto2));

        // Act
        List<AddressDto> result = addressService.getAddresses(userEmail);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(testAddressDto1.getAddressId(), result.get(0).getAddressId());
        assertEquals(testAddressDto2.getAddressId(), result.get(1).getAddressId());

        // Verify
        verify(userRepository, times(1)).findByEmail(userEmail);
        verify(orderMapper, times(1)).addressesToAddressDtos(testUser.getAddresses());
    }

    @Test
    void getAddresses_userNotFound() {
        // Arrange
        String userEmail = "nonexistent@example.com";

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException thrown = assertThrows(NotFoundException.class, () -> {
            addressService.getAddresses(userEmail);
        });

        assertEquals("User not found", thrown.getMessage()); // Corrected assertion

        // Verify
        verify(userRepository, times(1)).findByEmail(userEmail);
        verify(orderMapper, never()).addressesToAddressDtos(anyList());
    }

    @Test
    void deleteAddressById_admin_success() {
        // Arrange
        Long userId = testUser.getUserId();
        Long addressId = testAddress1.getAddressId();
        testUser.getAddresses().add(testAddress1); // Ensure user has the address

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(testAddress1));
        when(orderRepository.findByAddress(any(Address.class))).thenReturn(new ArrayList<>());
        when(orderMapper.addressToAddressDto(testAddress1)).thenReturn(testAddressDto1);

        // Act
        AddressDto result = addressService.deleteAddressById(userId, addressId);

        // Assert
        assertNotNull(result);
        assertEquals(testAddressDto1.getAddressId(), result.getAddressId());
        assertFalse(testUser.getAddresses().contains(testAddress1)); // Verify address removed from user's list

        // Verify
        verify(userRepository, times(1)).findById(userId);
        verify(addressRepository, times(1)).findById(addressId);
        verify(userRepository, times(1)).save(testUser);
        verify(orderMapper, times(1)).addressToAddressDto(testAddress1);
    }

    @Test
    void deleteAddressById_admin_userNotFound() {
        // Arrange
        Long userId = 99L;
        Long addressId = testAddress1.getAddressId();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException thrown = assertThrows(NotFoundException.class, () -> {
            addressService.deleteAddressById(userId, addressId);
        });

        assertEquals("User not found", thrown.getMessage()); // Corrected assertion

        // Verify
        verify(userRepository, times(1)).findById(userId);
        verify(addressRepository, never()).findById(anyLong());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deleteAddressById_admin_addressNotFound() {
        // Arrange
        Long userId = testUser.getUserId();
        Long addressId = 999L;

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(addressRepository.findById(addressId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException thrown = assertThrows(NotFoundException.class, () -> {
            addressService.deleteAddressById(userId, addressId);
        });

        assertEquals("Address not found", thrown.getMessage()); // Corrected assertion

        // Verify
        verify(userRepository, times(1)).findById(userId);
        verify(addressRepository, times(1)).findById(addressId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deleteAddressById_admin_addressNotBelongToUser() {
        // Arrange
        Long userId = testUser.getUserId();
        Long addressId = testAddress1.getAddressId();
        // Do NOT add testAddress1 to testUser.getAddresses()

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(testAddress1));

        // Act & Assert
        NotFoundException thrown = assertThrows(NotFoundException.class, () -> {
            addressService.deleteAddressById(userId, addressId);
        });

        assertEquals("Address not found", thrown.getMessage()); // Corrected assertion

        // Verify
        verify(userRepository, times(1)).findById(userId);
        verify(addressRepository, times(1)).findById(addressId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deleteAddressById_customer_success() {
        // Arrange
        String userEmail = testUser.getEmail();
        Long addressId = testAddress1.getAddressId();
        testUser.getAddresses().add(testAddress1); // Ensure user has the address

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(testAddress1));
        when(orderMapper.addressToAddressDto(testAddress1)).thenReturn(testAddressDto1);

        // Act
        AddressDto result = addressService.deleteAddressById(addressId, userEmail);

        // Assert
        assertNotNull(result);
        assertEquals(testAddressDto1.getAddressId(), result.getAddressId());
        assertFalse(testUser.getAddresses().contains(testAddress1)); // Verify address removed from user's list

        // Verify
        verify(userRepository, times(1)).findByEmail(userEmail);
        verify(addressRepository, times(1)).findById(addressId);
        verify(userRepository, times(1)).save(testUser);
        verify(orderMapper, times(1)).addressToAddressDto(testAddress1);
    }

    @Test
    void deleteAddressById_customer_userNotFound() {
        // Arrange
        String userEmail = "nonexistent@example.com";
        Long addressId = testAddress1.getAddressId();

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException thrown = assertThrows(NotFoundException.class, () -> {
            addressService.deleteAddressById(addressId, userEmail);
        });

        assertEquals("User not found", thrown.getMessage()); // Corrected assertion

        // Verify
        verify(userRepository, times(1)).findByEmail(userEmail);
        verify(addressRepository, never()).findById(anyLong());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deleteAddressById_customer_addressNotFound() {
        // Arrange
        String userEmail = testUser.getEmail();
        Long addressId = 999L;

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(addressRepository.findById(addressId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException thrown = assertThrows(NotFoundException.class, () -> {
            addressService.deleteAddressById(addressId, userEmail);
        });

        assertEquals("Address not found", thrown.getMessage()); // Corrected assertion

        // Verify
        verify(userRepository, times(1)).findByEmail(userEmail);
        verify(addressRepository, times(1)).findById(addressId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deleteAddressById_customer_addressNotBelongToUser() {
        // Arrange
        String userEmail = testUser.getEmail();
        Long addressId = testAddress1.getAddressId();
        // Do NOT add testAddress1 to testUser.getAddresses()

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(testAddress1));

        // Act & Assert
        NotFoundException thrown = assertThrows(NotFoundException.class, () -> {
            addressService.deleteAddressById(addressId, userEmail);
        });

        assertEquals("Address not found", thrown.getMessage()); // Corrected assertion

        // Verify
        verify(userRepository, times(1)).findByEmail(userEmail);
        verify(addressRepository, times(1)).findById(addressId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateAddressByEmail_success() {
        // Arrange
        String userEmail = testUser.getEmail();
        Long addressId = testAddress1.getAddressId();
        testUser.getAddresses().add(testAddress1); // Ensure user has the address

        AddressDto updateDto = new AddressDto(addressId, "Updated Name", "Updated Country", "Updated City", "Updated Province", null, null, "Street", "Apartment", null);
        Address updatedAddress = new Address();
        updatedAddress.setAddressId(addressId);
        updatedAddress.setName("Updated Name");
        // ... set other updated fields

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(testAddress1));
        doNothing().when(orderMapper).updateAddressFromDto(updateDto, testAddress1); // Mock the void method
        when(addressRepository.save(testAddress1)).thenReturn(updatedAddress);
        when(orderMapper.addressToAddressDto(updatedAddress)).thenReturn(updateDto); // Map updated entity to DTO

        // Act
        AddressDto result = addressService.updateAddressByEmail(updateDto, userEmail);

        // Assert
        assertNotNull(result);
        assertEquals(updateDto.getAddressId(), result.getAddressId());
        assertEquals(updateDto.getName(), result.getName());
        assertEquals(updateDto.getCountry(), result.getCountry());

        // Verify
        verify(userRepository, times(1)).findByEmail(userEmail);
        verify(addressRepository, times(1)).findById(addressId);
        verify(orderMapper, times(1)).updateAddressFromDto(updateDto, testAddress1);
        verify(addressRepository, times(1)).save(testAddress1);
        verify(orderMapper, times(1)).addressToAddressDto(updatedAddress);
    }

    @Test
    void updateAddressByEmail_nullAddressId() {
        // Arrange
        String userEmail = testUser.getEmail();
        AddressDto updateDto = new AddressDto(null, "Updated Name", "Updated Country", "Updated City", "Updated Province", null, null, "Street", "Apartment", null);

        // Act & Assert
        NullValueException thrown = assertThrows(NullValueException.class, () -> addressService.updateAddressByEmail(updateDto, userEmail));

        assertEquals("Address id cannot be null", thrown.getMessage()); // Corrected assertion

        // Verify
        verify(userRepository, never()).findByEmail(anyString());
        verify(addressRepository, never()).findById(anyLong());
    }

    @Test
    void updateAddressByEmail_userNotFound() {
        // Arrange
        String userEmail = "nonexistent@example.com";
        Long addressId = testAddress1.getAddressId();
        AddressDto updateDto = new AddressDto(addressId, "Updated Name", "Updated Country", "Updated City", "Updated Province", null, null, "Street", "Apartment", null);

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException thrown = assertThrows(NotFoundException.class, () -> {
            addressService.updateAddressByEmail(updateDto, userEmail);
        });

        assertEquals("User not found", thrown.getMessage()); // Corrected assertion

        // Verify
        verify(userRepository, times(1)).findByEmail(userEmail);
        verify(addressRepository, never()).findById(anyLong());
    }

    @Test
    void updateAddressByEmail_addressNotFound() {
        // Arrange
        String userEmail = testUser.getEmail();
        Long addressId = 999L;
        AddressDto updateDto = new AddressDto(addressId, "Updated Name", "Updated Country", "Updated City", "Updated Province", null, null, "Street", "Apartment", null);

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(addressRepository.findById(addressId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException thrown = assertThrows(NotFoundException.class, () -> {
            addressService.updateAddressByEmail(updateDto, userEmail);
        });

        assertEquals("Address not found", thrown.getMessage()); // Corrected assertion

        // Verify
        verify(userRepository, times(1)).findByEmail(userEmail);
        verify(addressRepository, times(1)).findById(addressId);
    }

    @Test
    void updateAddressByEmail_addressNotBelongToUser() {
        // Arrange
        String userEmail = testUser.getEmail();
        Long addressId = testAddress1.getAddressId();
        // Do NOT add testAddress1 to testUser.getAddresses()

        AddressDto updateDto = new AddressDto(addressId, "Updated Name", "Updated Country", "Updated City", "Updated Province", null, null, "Street", "Apartment", null);

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(testAddress1));

        // Act & Assert
        NotFoundException thrown = assertThrows(NotFoundException.class, () -> {
            addressService.updateAddressByEmail(updateDto, userEmail);
        });

        assertEquals("Address not found", thrown.getMessage()); // Corrected assertion

        // Verify
        verify(userRepository, times(1)).findByEmail(userEmail);
        verify(addressRepository, times(1)).findById(addressId);
    }
}