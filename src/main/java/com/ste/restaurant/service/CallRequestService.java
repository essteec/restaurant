package com.ste.restaurant.service;

import com.ste.restaurant.dto.CallRequestDto;
import com.ste.restaurant.dto.CallRequestDtoBasic;
import com.ste.restaurant.entity.*;
import com.ste.restaurant.mapper.OrderMapper;
import com.ste.restaurant.repository.CallRequestRepository;
import com.ste.restaurant.repository.OrderRepository;
import com.ste.restaurant.repository.UserRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CallRequestService {
    @Autowired
    private CallRequestRepository callRequestRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderMapper orderMapper;

    public CallRequestDto getCallRequestById(Long id) {
        Optional<CallRequest> callRequest = callRequestRepository.findById(id);
        if (callRequest.isEmpty()) {
            return null;
        }
        return orderMapper.callRequestToCallRequestDto(callRequest.get());
    }

    public CallRequestDto deleteCallRequestById(Long id) {
        Optional<CallRequest> callRequestOpt = callRequestRepository.findById(id);
        if (callRequestOpt.isEmpty()) {
            return null;
        }
        CallRequest callRequest = callRequestOpt.get();
        callRequestRepository.delete(callRequest);
        return orderMapper.callRequestToCallRequestDto(callRequest);
    }

    public List<CallRequestDto> getAllCallRequests() {
        List<CallRequest> calls = callRequestRepository.findAll();
        List<CallRequestDto> dtos = new ArrayList<>();
        for (CallRequest callRequest : calls) {
            CallRequestDto callRequestDto = orderMapper.callRequestToCallRequestDto(callRequest);
            dtos.add(callRequestDto);
        }

        return dtos;
    }

    public List<CallRequestDto> getAllCallRequestsBy(String type) {
        RequestType requestType;
        try {
            requestType = RequestType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid type " + type);
        }

        List<CallRequest> calls = callRequestRepository.findAllByType(requestType);
        List<CallRequestDto> dtos = new ArrayList<>();
        for (CallRequest callRequest : calls) {
            CallRequestDto callRequestDto = orderMapper.callRequestToCallRequestDto(callRequest);
            dtos.add(callRequestDto);
        }

        return dtos;
    }

    public List<CallRequestDto> getAllCallRequestsBy(boolean active) {
        List<CallRequest> calls = callRequestRepository.findAllByActive(active);
        List<CallRequestDto> dtos = new ArrayList<>();
        for (CallRequest callRequest : calls) {
            CallRequestDto callRequestDto = orderMapper.callRequestToCallRequestDto(callRequest);
            dtos.add(callRequestDto);
        }

        return dtos;
    }

    public List<CallRequestDto> getAllCallRequestsBy(String type, Boolean active) {
        RequestType requestType;
        try {
            requestType = RequestType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid type " + type);
        }

        List<CallRequest> calls = callRequestRepository.findAllByTypeAndActive(requestType, active);

        List<CallRequestDto> dtos = new ArrayList<>();
        for (CallRequest callRequest : calls) {
            CallRequestDto callRequestDto = orderMapper.callRequestToCallRequestDto(callRequest);
            dtos.add(callRequestDto);
        }

        return dtos;
    }

    public List<CallRequestDto> getLatestCallRequests(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

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
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        CallRequest callRequest = new CallRequest();
        BeanUtils.copyProperties(callRequestDtoBasic, callRequest);
        callRequest.setCustomer(user);
        callRequest.setCreatedAt(LocalDateTime.now());

        // To set the table in CallRequest, get the user's last order with status not COMPLETED and use its table.
        Order lastActiveOrder = orderRepository.findTopByCustomerAndStatusNotAndStatusNotOrderByOrderTimeDesc(user, OrderStatus.COMPLETED, OrderStatus.CANCELLED);
        if (lastActiveOrder == null || lastActiveOrder.getTable() == null) {
            throw new IllegalStateException("No active order with a table found for user");
        }
        callRequest.setTable(lastActiveOrder.getTable());
        
        callRequestRepository.save(callRequest);
        CallRequestDto response = orderMapper.callRequestToCallRequestDto(callRequest);
        response.setCustomer(null);
        return response;
    }

    public CallRequestDto disableCallRequestById(Long id) {
        Optional<CallRequest> callRequestOpt = callRequestRepository.findById(id);
        if (callRequestOpt.isEmpty()) {
            return null;
        }
        CallRequest callRequest = callRequestOpt.get();
        callRequest.setActive(false);

        callRequestRepository.save(callRequest);
        return orderMapper.callRequestToCallRequestDto(callRequest);
    }

    public CallRequestDto disableCallRequestById(Long id, String email) {
        Optional<CallRequest> callRequestOpt = callRequestRepository.findById(id);
        if (callRequestOpt.isEmpty()) {
            return null;
        }
        CallRequest callRequest = callRequestOpt.get();

        if (!callRequest.getCustomer().getEmail().equals(email)) {
            throw new AccessDeniedException("Not allowed to disable this call request");
        }

        callRequest.setActive(false);
        callRequestRepository.save(callRequest);

        return orderMapper.callRequestToCallRequestDto(callRequest);
    }
}