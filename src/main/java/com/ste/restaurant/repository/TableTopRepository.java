package com.ste.restaurant.repository;

import com.ste.restaurant.entity.enums.TableStatus;
import com.ste.restaurant.entity.TableTop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TableTopRepository extends JpaRepository<TableTop, Long> {
    boolean existsTableTopByTableNumber(String tableNumber);

    Optional<TableTop> findByTableNumber(String tableNumber);

    List<TableTop> findAllByTableStatus(TableStatus tableStatus);

    void deleteByTableNumber(String name);
}