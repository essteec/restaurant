package com.ste.restaurant.repository;

import com.ste.restaurant.entity.CallRequest;
import com.ste.restaurant.entity.RequestType;
import com.ste.restaurant.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CallRequestRepository extends JpaRepository<CallRequest, Long> {

    List<CallRequest> findByCustomerAndCreatedAtAfter(User user, LocalDateTime createdAtAfter);

    Page<CallRequest> findAllByType(RequestType type, Pageable pageable);

    Page<CallRequest> findAllByActive(boolean active, Pageable pageable);

    Page<CallRequest> findAllByTypeAndActive(RequestType type, boolean isActive, Pageable pageable);
}