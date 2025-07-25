package com.ste.restaurant.repository;

import com.ste.restaurant.entity.CallRequest;
import com.ste.restaurant.entity.RequestType;
import com.ste.restaurant.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CallRequestRepository extends JpaRepository<CallRequest, Long> {
    List<CallRequest> findAllByType(RequestType type);

    List<CallRequest> findAllByActive(boolean active);

    List<CallRequest> findAllByTypeAndActive(RequestType type, boolean isActive);

    List<CallRequest> findByCustomerAndCreatedAtAfter(User user, LocalDateTime createdAtAfter);
}