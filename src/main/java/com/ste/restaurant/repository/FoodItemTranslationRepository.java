package com.ste.restaurant.repository;

import com.ste.restaurant.dto.FoodItemTranslationDto;
import com.ste.restaurant.entity.FoodItem;
import com.ste.restaurant.entity.FoodItemTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodItemTranslationRepository extends JpaRepository<FoodItemTranslation, Long> {
    @Query("SELECT COUNT(DISTINCT t.languageCode) FROM FoodItemTranslation t")
    long countDistinctLanguages();

    boolean existsByLanguageCode(String languageCode);
}
