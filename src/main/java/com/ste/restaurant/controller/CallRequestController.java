package com.ste.restaurant.controller;

import com.ste.restaurant.dto.CallRequestDto;
import com.ste.restaurant.dto.CallRequestDtoBasic;
import com.ste.restaurant.service.CallRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rest/api/call-requests")
public class CallRequestController {

    @Autowired
    private CallRequestService callRequestService;

    // Admin
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(path = "/{id}")
    public CallRequestDto getCallRequestById(@PathVariable Long id) {
        return callRequestService.getCallRequestById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(path = "/{id}")
    public CallRequestDto deleteCallRequestById(@PathVariable Long id) {
        return callRequestService.deleteCallRequestById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF', 'WAITER')")
    @GetMapping
    public List<CallRequestDto> getAllCallRequests(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean active,
            Authentication auth) {
        if (type != null &&  active != null) {
            return callRequestService.getAllCallRequestsBy(type, active);
        }  else if (type != null) {
            return callRequestService.getAllCallRequestsBy(type);
        } else if (active != null) {
            return callRequestService.getAllCallRequestsBy(active);
        } else  {
            return callRequestService.getAllCallRequests();
        }
    }

    // Customer
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VIP_CUSTOMER')")
    @GetMapping(path = "/my/latest")
    public List<CallRequestDto> getLatestCallRequests(Authentication auth) {
        return callRequestService.getLatestCallRequests(auth.getName());
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public CallRequestDto createCallRequest(@RequestBody CallRequestDtoBasic callRequestDto, Authentication auth) {
        return callRequestService.createCallRequest(callRequestDto, auth.getName());
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping(path = "/{id}")
    public CallRequestDto disableCallRequestByCustomer(@PathVariable Long id, Authentication auth) {
        return callRequestService.disableCallRequestById(id, auth.getName());
    }

    // waiter or admin
    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER')")
    @PatchMapping(path = "/{id}/admin")
    public CallRequestDto disableCallRequestByAdmin(@PathVariable Long id) {
        return  callRequestService.disableCallRequestById(id);
    }
}