package com.ste.restaurant.repository;

import com.ste.restaurant.entity.Address;
import com.ste.restaurant.entity.User;
import com.ste.restaurant.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    void deleteByEmail(String email);

    List<User> findAllByRole(UserRole role);
}
