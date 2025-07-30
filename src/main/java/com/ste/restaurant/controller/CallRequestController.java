package com.ste.restaurant.controller;

import com.ste.restaurant.dto.CallRequestDto;
import com.ste.restaurant.dto.CallRequestDtoBasic;
import com.ste.restaurant.service.CallRequestService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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

    // waiter or admin
    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER')")
    @GetMapping
    public List<CallRequestDto> getAllCallRequests(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean active,
            Authentication auth) {
        if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_WAITER")))
            return callRequestService.getAllCallRequestsBy(true);
        if (type != null &&  active != null)
            return callRequestService.getAllCallRequestsBy(type, active);
        if (type != null)
            return callRequestService.getAllCallRequestsBy(type);
        if (active != null)
            return callRequestService.getAllCallRequestsBy(active);
        return callRequestService.getAllCallRequests();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER')")
    @PatchMapping(path = "/{id}/resolve")
    public CallRequestDto resolveCallRequest(@PathVariable Long id) {
        return  callRequestService.resolveCallRequestById(id);
    }

    // Customer
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping(path = "/my/latest")
    public List<CallRequestDto> getLatestCallRequests(Authentication auth) {
        return callRequestService.getLatestCallRequests(auth.getName());
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping
    public CallRequestDto createCallRequest(@Valid @RequestBody CallRequestDtoBasic callRequestDto, Authentication auth) {
        return callRequestService.createCallRequest(callRequestDto, auth.getName());
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @PatchMapping(path = "/{id}")
    public CallRequestDto resolveCallRequestByCustomer(@PathVariable Long id, Authentication auth) {
        return callRequestService.resolveCallRequestById(id, auth.getName());
    }
}