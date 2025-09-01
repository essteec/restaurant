package com.ste.restaurant.repository;

import com.ste.restaurant.dto.FoodItemTranslationDto;
import com.ste.restaurant.entity.FoodItem;
import com.ste.restaurant.entity.FoodItemTranslation;
import com.ste.restaurant.entity.id.FoodItemTranslationId;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface FoodItemTranslationRepository extends JpaRepository<FoodItemTranslation, FoodItemTranslationId> {

    @Query("SELECT COUNT(DISTINCT t.foodItemTranslationId.languageCode) FROM FoodItemTranslation t")
    long countDistinctLanguages();

    // list distinct languages
    @Query("SELECT DISTINCT t.foodItemTranslationId.languageCode AS code FROM FoodItemTranslation t")
    List<String> findDistinctLanguages();

    boolean existsByFoodItemTranslationId_LanguageCode(String foodItemTranslationIdLanguageCode);

    boolean existsByFoodItemTranslationId_FoodItemIdAndFoodItemTranslationId_LanguageCode(Long foodItemTranslationIdFoodItemId, String foodItemTranslationIdLanguageCode);

    List<FoodItemTranslation> findByFoodItemTranslationId_LanguageCode(String foodItemTranslationIdLanguageCode);
}
