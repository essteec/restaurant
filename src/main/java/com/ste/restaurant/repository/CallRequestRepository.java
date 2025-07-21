package com.ste.restaurant.repository;

import com.ste.restaurant.entity.CallRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CallRequestRepository extends JpaRepository<CallRequest, Long> {
} 