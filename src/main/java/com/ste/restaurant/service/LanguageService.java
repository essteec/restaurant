package com.ste.restaurant.service;

import com.ste.restaurant.repository.CategoryTranslationRepository;
import com.ste.restaurant.repository.FoodItemTranslationRepository;
import org.springframework.stereotype.Service;

@Service
public class LanguageService {

    private final FoodItemTranslationRepository foodItemTranslationRepository;
    private final CategoryTranslationRepository categoryTranslationRepository;

    public LanguageService(FoodItemTranslationRepository foodItemTranslationRepository,
                           CategoryTranslationRepository categoryTranslationRepository) {
        this.foodItemTranslationRepository = foodItemTranslationRepository;
        this.categoryTranslationRepository = categoryTranslationRepository;
    }

    public long countDistinctLanguages() {
        long foodItemLanguages = foodItemTranslationRepository.countDistinctLanguages();
        long categoryLanguages = categoryTranslationRepository.countDistinctLanguages();
        return Math.max(foodItemLanguages, categoryLanguages);
    }

    public boolean existsByLanguageCode(String langCode) {
        return foodItemTranslationRepository.existsByLanguageCode(langCode) ||
               categoryTranslationRepository.existsByLanguageCode(langCode);
    }
}
