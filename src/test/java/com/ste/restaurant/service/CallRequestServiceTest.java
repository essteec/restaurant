package com.ste.restaurant.service;

import com.ste.restaurant.dto.CallRequestDto;
import com.ste.restaurant.dto.CallRequestDtoBasic;
import com.ste.restaurant.dto.TableTopDto;
import com.ste.restaurant.dto.userdto.UserDto;
import com.ste.restaurant.entity.*;
import com.ste.restaurant.entity.enums.OrderStatus;
import com.ste.restaurant.entity.enums.RequestType;
import com.ste.restaurant.entity.enums.TableStatus;
import com.ste.restaurant.entity.enums.UserRole;
import com.ste.restaurant.exception.InvalidValueException;
import com.ste.restaurant.exception.NotFoundException;
import com.ste.restaurant.mapper.OrderMapper;
import com.ste.restaurant.repository.CallRequestRepository;
import com.ste.restaurant.repository.OrderRepository;
import com.ste.restaurant.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CallRequestServiceTest {

    @Mock
    private CallRequestRepository callRequestRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private CallRequestService callRequestService;

    private CallRequest testCallRequest;
    private CallRequestDto testCallRequestDto;
    private CallRequestDtoBasic testCallRequestDtoBasic;
    private User testUser;
    private UserDto testUserDto;
    private TableTop testTable;
    private TableTopDto testTableDto;
    private Order testOrder;
    private Pageable pageable;
    private LocalDateTime testDateTime;

    @BeforeEach
    void setUp() {
        testDateTime = LocalDateTime.of(2025, 8, 7, 12, 0, 0);

        // Test user
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setEmail("john.doe@example.com");
        testUser.setRole(UserRole.CUSTOMER);

        testUserDto = new UserDto();
        testUserDto.setFirstName("John");
        testUserDto.setLastName("Doe");
        testUserDto.setEmail("john.doe@example.com");
        testUserDto.setRole(UserRole.CUSTOMER);

        // Test table
        testTable = new TableTop();
        testTable.setTableId(1L);
        testTable.setTableNumber("T01");
        testTable.setCapacity(4);
        testTable.setTableStatus(TableStatus.OCCUPIED);

        testTableDto = new TableTopDto();
        testTableDto.setTableNumber("T01");
        testTableDto.setCapacity(4);
        testTableDto.setTableStatus(TableStatus.OCCUPIED);

        // Test order
        testOrder = new Order();
        testOrder.setOrderId(1L);
        testOrder.setCustomer(testUser);
        testOrder.setTable(testTable);
        testOrder.setStatus(OrderStatus.PREPARING);
        testOrder.setOrderTime(testDateTime.minusHours(1));

        // Test call request
        testCallRequest = new CallRequest();
        testCallRequest.setCallRequestId(1L);
        testCallRequest.setType(RequestType.WATER);
        testCallRequest.setMessage("Need water please");
        testCallRequest.setActive(true);
        testCallRequest.setCustomer(testUser);
        testCallRequest.setTable(testTable);
        testCallRequest.setCreatedAt(testDateTime);

        // Test call request DTO
        testCallRequestDto = new CallRequestDto();
        testCallRequestDto.setCallRequestId(1L);
        testCallRequestDto.setType(RequestType.WATER);
        testCallRequestDto.setMessage("Need water please");
        testCallRequestDto.setCustomer(testUserDto);
        testCallRequestDto.setTable(testTableDto);
        testCallRequestDto.setCreatedAt(testDateTime);

        // Test call request DTO basic
        testCallRequestDtoBasic = new CallRequestDtoBasic();
        testCallRequestDtoBasic.setType(RequestType.WATER);
        testCallRequestDtoBasic.setMessage("Need water please");

        pageable = PageRequest.of(0, 10);
    }

    @Test
    void getCallRequestById_success() {
        // Arrange
        when(callRequestRepository.findById(1L)).thenReturn(Optional.of(testCallRequest));
        when(orderMapper.callRequestToCallRequestDto(testCallRequest)).thenReturn(testCallRequestDto);

        // Act
        CallRequestDto result = callRequestService.getCallRequestById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCallRequestId()).isEqualTo(1L);
        assertThat(result.getType()).isEqualTo(RequestType.WATER);
        assertThat(result.getMessage()).isEqualTo("Need water please");
        verify(callRequestRepository).findById(1L);
        verify(orderMapper).callRequestToCallRequestDto(testCallRequest);
    }

    @Test
    void getCallRequestById_notFound() {
        // Arrange
        when(callRequestRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> callRequestService.getCallRequestById(999L))
                .isInstanceOf(NotFoundException.class);
        verify(callRequestRepository).findById(999L);
    }

    @Test
    void deleteCallRequestById_success() {
        // Arrange
        when(callRequestRepository.findById(1L)).thenReturn(Optional.of(testCallRequest));
        when(orderMapper.callRequestToCallRequestDto(testCallRequest)).thenReturn(testCallRequestDto);

        // Act
        CallRequestDto result = callRequestService.deleteCallRequestById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCallRequestId()).isEqualTo(1L);
        verify(callRequestRepository).findById(1L);
        verify(callRequestRepository).delete(testCallRequest);
        verify(orderMapper).callRequestToCallRequestDto(testCallRequest);
    }

    @Test
    void deleteCallRequestById_notFound() {
        // Arrange
        when(callRequestRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> callRequestService.deleteCallRequestById(999L))
                .isInstanceOf(NotFoundException.class);
        verify(callRequestRepository).findById(999L);
        verify(callRequestRepository, never()).delete(any());
    }

    @Test
    void getAllCallRequests_success() {
        // Arrange
        List<CallRequest> callRequests = Arrays.asList(testCallRequest);
        Page<CallRequest> callRequestPage = new PageImpl<>(callRequests, pageable, 1);
        when(callRequestRepository.findAll(pageable)).thenReturn(callRequestPage);
        when(orderMapper.callRequestToCallRequestDto(testCallRequest)).thenReturn(testCallRequestDto);

        // Act
        Page<CallRequestDto> result = callRequestService.getAllCallRequests(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getType()).isEqualTo(RequestType.WATER);
        verify(callRequestRepository).findAll(pageable);
    }

    @Test
    void getAllCallRequests_emptyResult() {
        // Arrange
        Page<CallRequest> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(callRequestRepository.findAll(pageable)).thenReturn(emptyPage);

        // Act
        Page<CallRequestDto> result = callRequestService.getAllCallRequests(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        verify(callRequestRepository).findAll(pageable);
    }

    @Test
    void getAllCallRequestsByType_success() {
        // Arrange
        List<CallRequest> callRequests = Arrays.asList(testCallRequest);
        Page<CallRequest> callRequestPage = new PageImpl<>(callRequests, pageable, 1);
        when(callRequestRepository.findAllByType(RequestType.WATER, pageable)).thenReturn(callRequestPage);
        when(orderMapper.callRequestToCallRequestDto(testCallRequest)).thenReturn(testCallRequestDto);

        // Act
        Page<CallRequestDto> result = callRequestService.getAllCallRequestsBy("WATER", pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getType()).isEqualTo(RequestType.WATER);
        verify(callRequestRepository).findAllByType(RequestType.WATER, pageable);
    }

    @Test
    void getAllCallRequestsByType_invalidType() {
        // Act & Assert
        assertThatThrownBy(() -> callRequestService.getAllCallRequestsBy("INVALID_TYPE", pageable))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void getAllCallRequestsByType_caseInsensitive() {
        // Arrange
        List<CallRequest> callRequests = Arrays.asList(testCallRequest);
        Page<CallRequest> callRequestPage = new PageImpl<>(callRequests, pageable, 1);
        when(callRequestRepository.findAllByType(RequestType.WATER, pageable)).thenReturn(callRequestPage);
        when(orderMapper.callRequestToCallRequestDto(testCallRequest)).thenReturn(testCallRequestDto);

        // Act
        Page<CallRequestDto> result = callRequestService.getAllCallRequestsBy("water", pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(callRequestRepository).findAllByType(RequestType.WATER, pageable);
    }

    @Test
    void getAllCallRequestsByActive_success() {
        // Arrange
        List<CallRequest> callRequests = Arrays.asList(testCallRequest);
        Page<CallRequest> callRequestPage = new PageImpl<>(callRequests, pageable, 1);
        when(callRequestRepository.findAllByActive(true, pageable)).thenReturn(callRequestPage);
        when(orderMapper.callRequestToCallRequestDto(testCallRequest)).thenReturn(testCallRequestDto);

        // Act
        Page<CallRequestDto> result = callRequestService.getAllCallRequestsBy(true, pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getType()).isEqualTo(RequestType.WATER);
        verify(callRequestRepository).findAllByActive(true, pageable);
    }

    @Test
    void getAllCallRequestsByActive_inactive() {
        // Arrange
        testCallRequest.setActive(false);
        List<CallRequest> callRequests = Arrays.asList(testCallRequest);
        Page<CallRequest> callRequestPage = new PageImpl<>(callRequests, pageable, 1);
        when(callRequestRepository.findAllByActive(false, pageable)).thenReturn(callRequestPage);
        when(orderMapper.callRequestToCallRequestDto(testCallRequest)).thenReturn(testCallRequestDto);

        // Act
        Page<CallRequestDto> result = callRequestService.getAllCallRequestsBy(false, pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(callRequestRepository).findAllByActive(false, pageable);
    }

    @Test
    void getAllCallRequestsByTypeAndActive_success() {
        // Arrange
        List<CallRequest> callRequests = Arrays.asList(testCallRequest);
        Page<CallRequest> callRequestPage = new PageImpl<>(callRequests, pageable, 1);
        when(callRequestRepository.findAllByTypeAndActive(RequestType.WATER, true, pageable)).thenReturn(callRequestPage);
        when(orderMapper.callRequestToCallRequestDto(testCallRequest)).thenReturn(testCallRequestDto);

        // Act
        Page<CallRequestDto> result = callRequestService.getAllCallRequestsBy("WATER", true, pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getType()).isEqualTo(RequestType.WATER);
        verify(callRequestRepository).findAllByTypeAndActive(RequestType.WATER, true, pageable);
    }

    @Test
    void getAllCallRequestsByTypeAndActive_invalidType() {
        // Act & Assert
        assertThatThrownBy(() -> callRequestService.getAllCallRequestsBy("INVALID_TYPE", true, pageable))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void getLatestCallRequests_success() {
        // Arrange
        List<CallRequest> callRequests = Arrays.asList(testCallRequest);
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser));
        when(callRequestRepository.findByCustomerAndCreatedAtAfter(eq(testUser), any(LocalDateTime.class)))
                .thenReturn(callRequests);
        when(orderMapper.callRequestToCallRequestDto(testCallRequest)).thenReturn(testCallRequestDto);

        // Act
        List<CallRequestDto> result = callRequestService.getLatestCallRequests("john.doe@example.com");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomer()).isNull(); // Customer should be set to null
        assertThat(result.get(0).getType()).isEqualTo(RequestType.WATER);
        verify(userRepository).findByEmail("john.doe@example.com");
        verify(callRequestRepository).findByCustomerAndCreatedAtAfter(eq(testUser), any(LocalDateTime.class));
        verify(orderMapper).callRequestToCallRequestDto(testCallRequest);
    }

    @Test
    void getLatestCallRequests_userNotFound() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> callRequestService.getLatestCallRequests("nonexistent@example.com"))
                .isInstanceOf(NotFoundException.class);
        verify(userRepository).findByEmail("nonexistent@example.com");
        verify(callRequestRepository, never()).findByCustomerAndCreatedAtAfter(any(), any());
    }

    @Test
    void getLatestCallRequests_emptyResult() {
        // Arrange
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser));
        when(callRequestRepository.findByCustomerAndCreatedAtAfter(eq(testUser), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // Act
        List<CallRequestDto> result = callRequestService.getLatestCallRequests("john.doe@example.com");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(userRepository).findByEmail("john.doe@example.com");
        verify(callRequestRepository).findByCustomerAndCreatedAtAfter(eq(testUser), any(LocalDateTime.class));
    }

    @Test
    void createCallRequest_success() {
        // Arrange
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser));
        when(orderMapper.callRequestDtoBasicToCallRequest(testCallRequestDtoBasic)).thenReturn(testCallRequest);
        when(orderRepository.findTopByCustomerAndStatusNotInOrderByOrderTimeDesc(testUser, Arrays.asList(OrderStatus.COMPLETED, OrderStatus.CANCELLED)))
                .thenReturn(testOrder);
        when(callRequestRepository.save(testCallRequest)).thenReturn(testCallRequest);
        when(orderMapper.callRequestToCallRequestDto(testCallRequest)).thenReturn(testCallRequestDto);

        // Act
        CallRequestDto result = callRequestService.createCallRequest(testCallRequestDtoBasic, "john.doe@example.com");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCustomer()).isNull(); // Customer should be set to null in response
        assertThat(result.getType()).isEqualTo(RequestType.WATER);
        verify(userRepository).findByEmail("john.doe@example.com");
        verify(orderMapper).callRequestDtoBasicToCallRequest(testCallRequestDtoBasic);
        verify(orderRepository).findTopByCustomerAndStatusNotInOrderByOrderTimeDesc(testUser, Arrays.asList(OrderStatus.COMPLETED, OrderStatus.CANCELLED));
        verify(callRequestRepository).save(testCallRequest);
        verify(orderMapper).callRequestToCallRequestDto(testCallRequest);
        
        // Verify the call request was properly set up
        assertThat(testCallRequest.getCustomer()).isEqualTo(testUser);
        assertThat(testCallRequest.getTable()).isEqualTo(testTable);
        assertThat(testCallRequest.getCreatedAt()).isNotNull();
    }

    @Test
    void createCallRequest_userNotFound() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> callRequestService.createCallRequest(testCallRequestDtoBasic, "nonexistent@example.com"))
                .isInstanceOf(NotFoundException.class);
        verify(userRepository).findByEmail("nonexistent@example.com");
        verify(callRequestRepository, never()).save(any());
    }

    @Test
    void createCallRequest_noActiveOrder() {
        // Arrange
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser));
        when(orderMapper.callRequestDtoBasicToCallRequest(testCallRequestDtoBasic)).thenReturn(testCallRequest);
        when(orderRepository.findTopByCustomerAndStatusNotInOrderByOrderTimeDesc(testUser, Arrays.asList(OrderStatus.COMPLETED, OrderStatus.CANCELLED)))
                .thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> callRequestService.createCallRequest(testCallRequestDtoBasic, "john.doe@example.com"))
                .isInstanceOf(NotFoundException.class);
        verify(userRepository).findByEmail("john.doe@example.com");
        verify(orderRepository).findTopByCustomerAndStatusNotInOrderByOrderTimeDesc(testUser, Arrays.asList(OrderStatus.COMPLETED, OrderStatus.CANCELLED));
        verify(callRequestRepository, never()).save(any());
    }

    @Test
    void createCallRequest_orderWithoutTable() {
        // Arrange
        testOrder.setTable(null); // Order has no table
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser));
        when(orderMapper.callRequestDtoBasicToCallRequest(testCallRequestDtoBasic)).thenReturn(testCallRequest);
        when(orderRepository.findTopByCustomerAndStatusNotInOrderByOrderTimeDesc(testUser, Arrays.asList(OrderStatus.COMPLETED, OrderStatus.CANCELLED)))
                .thenReturn(testOrder);

        // Act & Assert
        assertThatThrownBy(() -> callRequestService.createCallRequest(testCallRequestDtoBasic, "john.doe@example.com"))
                .isInstanceOf(NotFoundException.class);
        verify(userRepository).findByEmail("john.doe@example.com");
        verify(orderRepository).findTopByCustomerAndStatusNotInOrderByOrderTimeDesc(testUser, Arrays.asList(OrderStatus.COMPLETED, OrderStatus.CANCELLED));
        verify(callRequestRepository, never()).save(any());
    }

    @Test
    void resolveCallRequestById_employee_success() {
        // Arrange
        when(callRequestRepository.findById(1L)).thenReturn(Optional.of(testCallRequest));
        when(callRequestRepository.save(testCallRequest)).thenReturn(testCallRequest);
        when(orderMapper.callRequestToCallRequestDto(testCallRequest)).thenReturn(testCallRequestDto);

        // Act
        CallRequestDto result = callRequestService.resolveCallRequestById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCallRequestId()).isEqualTo(1L);
        verify(callRequestRepository).findById(1L);
        verify(callRequestRepository).save(testCallRequest);
        verify(orderMapper).callRequestToCallRequestDto(testCallRequest);
        assertThat(testCallRequest.isActive()).isFalse();
    }

    @Test
    void resolveCallRequestById_employee_notFound() {
        // Arrange
        when(callRequestRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> callRequestService.resolveCallRequestById(999L))
                .isInstanceOf(NotFoundException.class);
        verify(callRequestRepository).findById(999L);
        verify(callRequestRepository, never()).save(any());
    }

    @Test
    void resolveCallRequestById_customer_success() {
        // Arrange
        when(callRequestRepository.findById(1L)).thenReturn(Optional.of(testCallRequest));
        when(callRequestRepository.save(testCallRequest)).thenReturn(testCallRequest);
        when(orderMapper.callRequestToCallRequestDto(testCallRequest)).thenReturn(testCallRequestDto);

        // Act
        CallRequestDto result = callRequestService.resolveCallRequestById(1L, "john.doe@example.com");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCallRequestId()).isEqualTo(1L);
        verify(callRequestRepository).findById(1L);
        verify(callRequestRepository).save(testCallRequest);
        verify(orderMapper).callRequestToCallRequestDto(testCallRequest);
        assertThat(testCallRequest.isActive()).isFalse();
    }

    @Test
    void resolveCallRequestById_customer_notFound() {
        // Arrange
        when(callRequestRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> callRequestService.resolveCallRequestById(999L, "john.doe@example.com"))
                .isInstanceOf(NotFoundException.class);
        verify(callRequestRepository).findById(999L);
        verify(callRequestRepository, never()).save(any());
    }

    @Test
    void resolveCallRequestById_customer_wrongOwner() {
        // Arrange
        when(callRequestRepository.findById(1L)).thenReturn(Optional.of(testCallRequest));

        // Act & Assert
        assertThatThrownBy(() -> callRequestService.resolveCallRequestById(1L, "other@example.com"))
                .isInstanceOf(NotFoundException.class);
        verify(callRequestRepository).findById(1L);
        verify(callRequestRepository, never()).save(any());
    }

    @Test
    void resolveCallRequestById_customer_alreadyResolved() {
        // Arrange
        testCallRequest.setActive(false); // Already resolved
        when(callRequestRepository.findById(1L)).thenReturn(Optional.of(testCallRequest));
        when(callRequestRepository.save(testCallRequest)).thenReturn(testCallRequest);
        when(orderMapper.callRequestToCallRequestDto(testCallRequest)).thenReturn(testCallRequestDto);

        // Act
        CallRequestDto result = callRequestService.resolveCallRequestById(1L, "john.doe@example.com");

        // Assert
        assertThat(result).isNotNull();
        verify(callRequestRepository).findById(1L);
        verify(callRequestRepository).save(testCallRequest);
        assertThat(testCallRequest.isActive()).isFalse(); // Should remain false
    }

    @Test
    void createCallRequest_differentRequestTypes() {
        // Test different request types
        RequestType[] types = {RequestType.PAYMENT, RequestType.ASSISTANCE, RequestType.NEED, RequestType.PACK};
        
        for (RequestType type : types) {
            // Arrange
            CallRequestDtoBasic dtoBasic = new CallRequestDtoBasic();
            dtoBasic.setType(type);
            dtoBasic.setMessage("Test message for " + type);
            
            CallRequest callRequest = new CallRequest();
            callRequest.setType(type);
            callRequest.setMessage("Test message for " + type);
            
            when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser));
            when(orderMapper.callRequestDtoBasicToCallRequest(dtoBasic)).thenReturn(callRequest);
            when(orderRepository.findTopByCustomerAndStatusNotInOrderByOrderTimeDesc(testUser, Arrays.asList(OrderStatus.COMPLETED, OrderStatus.CANCELLED)))
                    .thenReturn(testOrder);
            when(callRequestRepository.save(callRequest)).thenReturn(callRequest);
            
            CallRequestDto responseDto = new CallRequestDto();
            responseDto.setType(type);
            responseDto.setMessage("Test message for " + type);
            when(orderMapper.callRequestToCallRequestDto(callRequest)).thenReturn(responseDto);

            // Act
            CallRequestDto result = callRequestService.createCallRequest(dtoBasic, "john.doe@example.com");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getType()).isEqualTo(type);
            assertThat(result.getMessage()).isEqualTo("Test message for " + type);
        }
    }
}
