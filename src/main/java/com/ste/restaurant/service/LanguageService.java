package com.ste.restaurant.service;

import com.ste.restaurant.repository.CategoryTranslationRepository;
import com.ste.restaurant.repository.FoodItemTranslationRepository;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        return foodItemTranslationRepository.existsByFoodItemTranslationId_LanguageCode(langCode) ||
               categoryTranslationRepository.existsByCategoryTranslationId_LanguageCode(langCode);
    }

    public List<String> getSupportedLanguages() {
        List<String> foodItemLanguages = foodItemTranslationRepository.findDistinctLanguages();
        List<String> categoryLanguages = categoryTranslationRepository.findDistinctLanguages();
        // concate to set and return these in one list
        return Stream.concat(foodItemLanguages.stream(), categoryLanguages.stream())
                     .distinct()
                     .collect(Collectors.toList());
    }
}
