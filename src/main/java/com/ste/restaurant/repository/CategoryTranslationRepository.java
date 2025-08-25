package com.ste.restaurant.repository;

import com.ste.restaurant.entity.CategoryTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryTranslationRepository extends JpaRepository<CategoryTranslation, Long> {

    @Query("SELECT COUNT(DISTINCT t.languageCode) FROM CategoryTranslation t")
    long countDistinctLanguages();

    boolean existsByLanguageCode(String languageCode);
}
