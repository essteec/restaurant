package com.ste.restaurant.repository;

import com.ste.restaurant.entity.CategoryTranslation;
import com.ste.restaurant.entity.id.CategoryTranslationId;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryTranslationRepository extends JpaRepository<CategoryTranslation, CategoryTranslationId> {

    @Query("SELECT COUNT(DISTINCT t.categoryTranslationId.languageCode) FROM CategoryTranslation t")
    long countDistinctLanguages();

    // list distinct languages
    @Query("SELECT DISTINCT t.categoryTranslationId.languageCode AS code FROM CategoryTranslation t")
    List<String> findDistinctLanguages();

    boolean existsByCategoryTranslationId_LanguageCode(String categoryTranslationIdLanguageCode);

    boolean existsByCategoryTranslationId_CategoryIdAndCategoryTranslationId_LanguageCode(Long categoryTranslationIdCategoryId, String categoryTranslationIdLanguageCode);

    List<CategoryTranslation> findByCategoryTranslationId_LanguageCode(String categoryTranslationIdLanguageCode);
}
