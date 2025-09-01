package com.ste.restaurant.service;

import com.ste.restaurant.dto.CallRequestDto;
import com.ste.restaurant.dto.CallRequestDtoBasic;
import com.ste.restaurant.entity.*;
import com.ste.restaurant.entity.enums.OrderStatus;
import com.ste.restaurant.entity.enums.RequestType;
import com.ste.restaurant.exception.InvalidValueException;
import com.ste.restaurant.exception.NotFoundException;
import com.ste.restaurant.mapper.OrderMapper;
import com.ste.restaurant.repository.CallRequestRepository;
import com.ste.restaurant.repository.OrderRepository;
import com.ste.restaurant.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CallRequestService {
    private final CallRequestRepository callRequestRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;

    public CallRequestService(CallRequestRepository callRequestRepo, OrderRepository orderRepo,
                              UserRepository userRepo, OrderMapper orderMapper) {
        this.callRequestRepository = callRequestRepo;
        this.orderRepository = orderRepo;
        this.userRepository = userRepo;
        this.orderMapper = orderMapper;
    }

    public CallRequestDto getCallRequestById(Long id) {
        CallRequest callRequest = callRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("CallRequest", id));
        return orderMapper.callRequestToCallRequestDto(callRequest);
    }

    public CallRequestDto deleteCallRequestById(Long id) {
        CallRequest callRequest = callRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("CallRequest", id));

        callRequestRepository.delete(callRequest);
        return orderMapper.callRequestToCallRequestDto(callRequest);
    }

    public Page<CallRequestDto> getAllCallRequests(Pageable pageable) {
        Page<CallRequest> calls = callRequestRepository.findAll(pageable);
        return calls.map(orderMapper::callRequestToCallRequestDto);
    }

    public Page<CallRequestDto> getAllCallRequestsBy(String type, Pageable pageable) {
        RequestType requestType;
        try {
            requestType = RequestType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidValueException("CallRequest", "type", type);
        }

        Page<CallRequest> calls = callRequestRepository.findAllByType(requestType, pageable);

        return calls.map(orderMapper::callRequestToCallRequestDto);
    }

    public Page<CallRequestDto> getAllCallRequestsBy(boolean active, Pageable pageable) {
        Page<CallRequest> calls = callRequestRepository.findAllByActive(active, pageable);

        return calls.map(orderMapper::callRequestToCallRequestDto);
    }

    public Page<CallRequestDto> getAllCallRequestsBy(String type, Boolean active, Pageable pageable) {
        RequestType requestType;
        try {
            requestType = RequestType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidValueException("CallRequest", "type", type);
        }

        Page<CallRequest> calls = callRequestRepository.findAllByTypeAndActive(requestType, active, pageable);

        return calls.map(orderMapper::callRequestToCallRequestDto);
    }

    public List<CallRequestDto> getLatestCallRequests(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User", email));

        LocalDateTime since = LocalDateTime.now().minusHours(4);
        List<CallRequest> calls = callRequestRepository.findByCustomerAndCreatedAtAfter(user, since);

        List<CallRequestDto> callDtos = new ArrayList<>();
        for (CallRequest call : calls) {
            CallRequestDto callDto = orderMapper.callRequestToCallRequestDto(call);
            callDto.setCustomer(null);
            callDtos.add(callDto);
        }
        return callDtos;
    }

    public CallRequestDto createCallRequest(CallRequestDtoBasic callRequestDtoBasic, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User", email));

        CallRequest callRequest = orderMapper.callRequestDtoBasicToCallRequest(callRequestDtoBasic);
        callRequest.setCustomer(user);
        callRequest.setCreatedAt(LocalDateTime.now());

        // To set the table in CallRequest, get the user's last order with status not COMPLETED and use its table.
        Order lastActiveOrder = orderRepository.findTopByCustomerAndStatusNotInOrderByOrderTimeDesc(user, List.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED));
        if (lastActiveOrder == null || lastActiveOrder.getTable() == null) {
            throw new NotFoundException("ActiveOrder");
        }
        callRequest.setTable(lastActiveOrder.getTable());
        
        callRequestRepository.save(callRequest);
        CallRequestDto response = orderMapper.callRequestToCallRequestDto(callRequest);
        response.setCustomer(null);
        return response;
    }

    // employee
    public CallRequestDto resolveCallRequestById(Long id) {
        CallRequest callRequest = callRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("CallRequest", id));

        callRequest.setActive(false);
        callRequestRepository.save(callRequest);
        return orderMapper.callRequestToCallRequestDto(callRequest);
    }

    // customer
    public CallRequestDto resolveCallRequestById(Long id, String email) {
        CallRequest callRequest = callRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("CallRequest", id));

        if (!callRequest.getCustomer().getEmail().equals(email)) {
            throw new NotFoundException("CallRequest", id);
        }

        callRequest.setActive(false);
        callRequestRepository.save(callRequest);

        return orderMapper.callRequestToCallRequestDto(callRequest);
    }
}