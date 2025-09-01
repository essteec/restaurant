package com.ste.restaurant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TranslationPackDto {

    private String targetLanguage;

    private Map<String, List<String>> foodItemTranslations;

    private Map<String, String> categoryTranslations;
}
