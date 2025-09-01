package com.ste.restaurant.service;

import com.ste.restaurant.dto.TranslationPackDto;
import com.ste.restaurant.entity.Category;
import com.ste.restaurant.entity.CategoryTranslation;
import com.ste.restaurant.entity.FoodItem;
import com.ste.restaurant.entity.FoodItemTranslation;
import com.ste.restaurant.repository.CategoryRepository;
import com.ste.restaurant.repository.CategoryTranslationRepository;

import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.ste.restaurant.exception.InvalidValueException;
import com.ste.restaurant.repository.FoodItemRepository;
import com.ste.restaurant.repository.FoodItemTranslationRepository;

import jakarta.annotation.PreDestroy;
import jakarta.transaction.Transactional;

import org.springframework.cache.annotation.Cacheable;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@Profile("!test")
public class AiService {

    private final Client geminiClient;
    private final FoodItemRepository foodItemRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryTranslationRepository categoryTranslationRepository;
    private final FoodItemTranslationRepository foodItemTranslationRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService translationExecutor = Executors.newFixedThreadPool(3);
    
    // CACHE THE LANGUAGES MAP
    private volatile Map<String, String> cachedLanguages = null;


    public AiService(Client geminiClient, FoodItemRepository foodItemRepo, CategoryRepository categoryRepo, 
                     CategoryTranslationRepository categoryTranslationRepo, FoodItemTranslationRepository foodItemTranslationRepo) {
        this.geminiClient = geminiClient;
        this.foodItemRepository = foodItemRepo;
        this.categoryRepository = categoryRepo;
        this.categoryTranslationRepository = categoryTranslationRepo;
        this.foodItemTranslationRepository = foodItemTranslationRepo;
    }

    @Cacheable("foodDescriptions")
    public String getFoodDescriptionFromAi(String foodName) {
        String prompt = """
        You are a professional restaurant menu writer.
        Given the food name, generate a concise menu description.
        Return ONLY the description text - no JSON, no quotes, no extra formatting.
        
        Guidelines:
        - Keep it concise (4-16 words)
        - Do not include prices, cooking instructions, or promotional language
        - If it is not base food, highlight key ingredients and prominent flavors
        - If you cannot generate a description, return "No description available"
        
        Food name: %s
        
        Description:
        """.formatted(foodName);

        try {
            GenerateContentResponse response = geminiClient.models
                .generateContent("gemini-2.5-flash", prompt, null);
            System.out.println("AI response: " + response.text());

            String description = response.text();
    
            if (description == null || description.isEmpty() || description.equals("No description available")) {
                throw new InvalidValueException("Food item", "Food name", foodName);
            }
    
            return description.trim();

        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            throw new InvalidValueException("AI response", "Food name", foodName + " » " + e.getMessage());
        }
    }

        // pseudo flow:
        // - Validate input language
        // Food items: translate name and description to target languages
        // Categories: translate name to target languages

        // for each category at once:
        //   - filter out already translated categories
        //   - call gemini-2.0-flash with all the untranslated category names
        //   - get responses in format: { "source name": "translated name" ... }

        // for each food item with parallel requests:
        //   - filter out already translated food items
        //   - for each untranslated food item using ExecutorService:
        //   - call gemini-2.5-flash-lite
        //   - get responses in format: "<translated name>|<translated description>"
        //   - Handle rate limit: Catch rate limit exceptions and abort gracefully return partial results
        //   - Handle threads and add to result


        // if error from gemini api then abort the all the process and return the processed items as is
        //
        // important notes:
        //  we need to highly consider rate limits:
        //      -We need to make requests until hit the limit then abort and return the processed items as is with a warning
        //      -We cannot implement cache directly since we should continue from where we left off. we need it to be a mechanism for category too. bc it is checked the translation exists after making request.

    public TranslationPackDto translateAllTo(String language) {
        
        if (!validateLanguage(language)) {
            throw new InvalidValueException("Language", language);
        }

        if (language == null || language.trim().isEmpty()) {
            throw new InvalidValueException("Language", "empty: " + language);
        }

        List<FoodItem> foodItems = foodItemRepository.findAll();
        List<Category> categories = categoryRepository.findAll();

        TranslationPackDto result = new TranslationPackDto();
        result.setTargetLanguage(language);
        result.setCategoryTranslations(new HashMap<>());
        result.setFoodItemTranslations(new ConcurrentHashMap<>());

        AtomicBoolean rateLimitHit = new AtomicBoolean(false);

        try {
            int categoriesProcessed = processCategoryTranslations(categories, language, 
                                        result.getCategoryTranslations(), rateLimitHit);
                                        
            System.out.println("Processed categories: " + categoriesProcessed);

            if (rateLimitHit.get()) {
                System.out.println("WARNING: Rate limit hit! Aborting translation process before food items.");
                return result;
            }

            int foodItemsProcessed = processFoodItemTranslations(foodItems, language, 
                                        result.getFoodItemTranslations(), rateLimitHit);

            System.out.println("Processed food items: " + foodItemsProcessed);

        } catch (Exception e) {
            System.err.println("Error occurred during " + language + " language process: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }


    private int processCategoryTranslations(List<Category> categories, String language,
                                            Map<String, String> result, AtomicBoolean rateLimitHit) {
        if (categories.isEmpty()) {
            System.out.println("No categories to translate.");
            return 0;
        }

        List<Category> untranslatedCategories = categories.stream()
                .filter(cat -> !categoryTranslationRepository.existsByCategoryTranslationId_CategoryIdAndCategoryTranslationId_LanguageCode(
                        cat.getCategoryId(), getLanguageCode(language)))
                .collect(Collectors.toList());

        if (untranslatedCategories.isEmpty()) {
            System.out.println("All categories already translated to " + language);
            return 0;
        }

        for (int i = 0; i < untranslatedCategories.size(); i += 10) {
            List<Category> sublist = (untranslatedCategories.subList(i, Math.min(i + 10, untranslatedCategories.size())));
            if (rateLimitHit.get()) {
                System.out.println("Aborting further category translations due to rate limit hit.");
                break;
            }

            processCategoryBatch(sublist, language, result, rateLimitHit);
        }

        return result.size();
    }

    private int processFoodItemTranslations(List<FoodItem> foodItems, String language, 
                                            Map<String, List<String>> result, AtomicBoolean rateLimitHit) {
        if (foodItems.isEmpty()) {
            System.out.println("No food items to translate.");
            return 0;
        }

        List<FoodItem> untranslatedFoodItems = foodItems.stream()
                .filter(food -> !foodItemTranslationRepository.existsByFoodItemTranslationId_FoodItemIdAndFoodItemTranslationId_LanguageCode(
                        food.getFoodId(), getLanguageCode(language)))
                .toList();

        if (untranslatedFoodItems.isEmpty()) {
            System.out.println("All food items already translated to " + language);
            return 0;
        }

        System.out.println("Translating " + untranslatedFoodItems.size() + " food items to " + language + "...");

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (FoodItem food : untranslatedFoodItems) {
            if (rateLimitHit.get()) {
                System.out.println("Aborting further translations for " + food.getFoodName() + " due to rate limit hit.");
                break;
            }

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> 
                translateSingleFoodItemWithDelay(food, language, result, rateLimitHit),
                translationExecutor
            );

            futures.add(future);
        }

        try {
            System.out.println("Waiting for food item translations to complete...");

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(4, TimeUnit.MINUTES);

            System.out.println("Successfully translated " + result.size() + " food items to " + language);

        } catch (TimeoutException e) {
            System.err.println("Translation timed out after 4 minutes.");
            futures.forEach(future -> future.cancel(true));
            throw new RuntimeException("Translation timed out");
        } catch (InterruptedException e) {
            System.err.println("Translation interrupted: " + e.getMessage());
            e.printStackTrace();
            futures.forEach(future -> future.cancel(true));
            Thread.currentThread().interrupt();
            throw new RuntimeException("Translation interrupted");
        } catch (Exception e) {
            System.err.println("Error occurred while waiting for food items to translate to " + language + ": " + e.getMessage());
            e.printStackTrace();
            futures.forEach(future -> future.cancel(true));
        }

        return result.size();
    }

    private void processCategoryBatch(List<Category> categories, String language, Map<String, String> result, AtomicBoolean rateLimitHit) {
        try {
            String categoryNames = categories.stream()
                    .map(Category::getCategoryName)
                    .collect(Collectors.joining(", "));
                    
            String promptCategory = """
            You are a professional restaurant menu translator.
            Your task is to translate the given category names into target language: %s.
            
            Output requirements:
                Return results only as a valid JSON object in the format:
                {
                    "source name 1": "translated name 1",
                    "source name 2": "translated name 2"
                }
            
                If a category name cannot be translated, set its value to null.
            
                Do not include any text outside the JSON object.
            
            Category names to translate: %s
            """.formatted(language, categoryNames);

            System.out.println("«=-=» Generating prompt for category names: " + categoryNames);
            GenerateContentResponse categoryResponse = geminiClient.models
                .generateContent("gemini-2.0-flash", promptCategory, null);


            String categoryJson = categoryResponse.text();
            
            System.out.println("«<->» Received response for category names: " + categoryNames + ": " + categoryJson);


            if (categoryJson == null || categoryJson.isEmpty()) {
                System.err.println("Received empty or null JSON response for category names: " + categoryNames);
                return;
            }

            if (categoryJson.startsWith("```json")) {
                categoryJson = categoryJson.replaceAll("```json|```", "").trim();
            }

            // Parse JSON response
            Map<String, String> categoryTranslationsMap;
            try {
                categoryTranslationsMap = objectMapper.readValue(categoryJson, new TypeReference<Map<String, String>>() {});
            } catch (JsonProcessingException e) {
                System.err.println("Failed to parse category translation JSON: " + e.getMessage());
                System.err.println("Category JSON: " + categoryJson);
                e.printStackTrace();
                return;
            }

            for (Category category : categories) {
                String sourceName = category.getCategoryName();
                String translatedName = categoryTranslationsMap.get(sourceName);

                // if translated successfully
                if (translatedName != null && !translatedName.equalsIgnoreCase("null") && !translatedName.trim().isEmpty()) {
                    result.put(sourceName, translatedName.trim());
                }
            }

            System.out.println("Successfully translated " + result.size() + " categories to " + language);

        } catch (Exception e) {
            if (isRateLimitException(e)) {
                System.err.println("Warning: Rate limit hit during category translation!");
                rateLimitHit.set(true);
            }
            System.err.println("Error occurred while translating categories to " + language + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void translateSingleFoodItemWithDelay(FoodItem food, String language, Map<String, List<String>> result, AtomicBoolean rateLimitHit) {
        try {
            if (rateLimitHit.get()) {
                System.out.println("Rate limit exceeded, skipping translation for food item: " + food.getFoodName());
                return;
            }

            Thread.sleep(3000); // Introduce a delay of 3 seconds
            translateSingleFoodItem(food, language, result, rateLimitHit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Translation delay interrupted for food item " + food.getFoodName() + ": " + e.getMessage());
        }
    }

    private void translateSingleFoodItem(FoodItem food, String language, 
                                         Map<String, List<String>> result, 
                                         AtomicBoolean rateLimitHit) {

        try {
            String promptFoodItem = """
            You are a professional restaurant menu translator.
            Translate the following food item name and description into target language: %s.

            Return only one line in the output format:
                translated name | translated description
            Rules:
                If the name or description cannot be translated, return "No translation available" in its place.
                For descriptions:
                    Make the translation NATURAL for %s native speakers.
                    Keep it CONCISE (8-25 words).
                    Highlight key ingredients and prominent flavors.
                    Do not add extra text or explanations outside the required output.
            Food name and description: %s | %s
            """.formatted(language, language, food.getFoodName(), food.getDescription());

            System.out.println("«=-=» Generating prompt for food item: " + food.getFoodName());
            GenerateContentResponse foodResponse = geminiClient.models
                .generateContent("gemini-2.5-flash-lite", promptFoodItem, null);

            System.out.println("«<->» Received response for food item: " + food.getFoodName() + ": " + foodResponse.text());

            String foodText = foodResponse.text();
            
            if (foodText == null) {
                System.err.println("Null translation response for food item '" + food.getFoodName() + "': " + null);
                return;
            }

            String[] foodParts = foodText.split("\\|");

            if (foodParts.length != 2) {
                System.err.println("Unexpected format in translation response for food item '" + food.getFoodName() + "': " + foodText);
                return;
            }

            String translatedName = foodParts[0].trim();
            String translatedDescription = foodParts[1].trim();

            if (translatedName.equals("No translation available") || translatedName.isBlank()) {
                System.err.println("No valid translation available for food item '" + food.getFoodName() + "'");
                return;
            }
            if (translatedDescription.equals("No translation available")) {
                translatedDescription = null;
            } 

            result.put(food.getFoodName(), List.of(translatedName, translatedDescription));
            System.out.println("Translated food item: " + food.getFoodName() + " to " + language);

        } catch (Exception e) {
            if (isRateLimitException(e)) {
                System.err.println("Rate limit exceeded while translating food item '" + food.getFoodName() + "': " + e.getMessage());
                rateLimitHit.set(true);
            }
            System.err.println("Error translating food item '" + food.getFoodName() + "': " + e.getMessage());
        }
    }

    private boolean isRateLimitException(Exception e) {
        String message = e.getMessage();
        if (message == null) return false;
        System.out.println("</> limit check on message: " + message);
        return message.contains("429") || 
               message.toLowerCase().contains("rate limit") ||
               message.toLowerCase().contains("quota exceeded") ||
               message.toLowerCase().contains("resource exhausted") ||
               message.toLowerCase().contains("resource_exhausted");
    }

    @PreDestroy
    public void shutdownExecutor() {
        translationExecutor.shutdown();

        try {
            if (!translationExecutor.awaitTermination(20, TimeUnit.SECONDS)) {
                translationExecutor.shutdownNow();
                if (!translationExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    System.err.println("Executor service did not terminate");
                }
            }
        } catch (InterruptedException e) {
            translationExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Transactional
    public boolean finalizeTranslations(TranslationPackDto translationPack) {
        if (translationPack == null || !validateLanguage(translationPack.getTargetLanguage())) {
            throw new InvalidValueException("Language", "empty: " + translationPack);
        }

        Map<String, List<String>> foodInput = translationPack.getFoodItemTranslations() != null
                ? translationPack.getFoodItemTranslations()
                : Map.of();
        Map<String, String> categoryInput = translationPack.getCategoryTranslations() != null
                ? translationPack.getCategoryTranslations()
                : Map.of();

        List<FoodItemTranslation> foodTranslations = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : foodInput.entrySet()) {
            String sourceName = entry.getKey();
            List<String> translatedValues = entry.getValue();
            // Process each translation

            FoodItem food = foodItemRepository.findByFoodName(sourceName).orElse(null);
            if (food == null) {
                System.err.println("Food item not found for name: " + sourceName);
                continue;
            }

            FoodItemTranslation translation = new FoodItemTranslation();
            if (translatedValues == null || translatedValues.isEmpty() || translatedValues.get(0) == null || translatedValues.get(0).isBlank()) {
                // skip empty/invalid entries
                continue;
            }
            translation.setName(translatedValues.get(0).trim());
            if (translatedValues.size() > 1 && translatedValues.get(1) != null && !translatedValues.get(1).isBlank()) {
                translation.setDescription(translatedValues.get(1));
            }
            translation.setFoodItem(food);
            translation.setId(getAvailableLanguages().get(translationPack.getTargetLanguage()), food);

            foodTranslations.add(translation);
        }

        List<CategoryTranslation> categoryTranslations = new ArrayList<>();

        for (Map.Entry<String, String> entry : categoryInput.entrySet()) {
            String sourceName = entry.getKey();
            String translatedName = entry.getValue();

            Category category = categoryRepository.findByCategoryName(sourceName).orElse(null);
            if (category == null) {
                System.err.println("Category not found for name: " + sourceName);
                continue;
            }

            if (translatedName == null || translatedName.isBlank()) {
                continue;
            }
            CategoryTranslation translation = new CategoryTranslation();
            translation.setName(translatedName.trim());
            translation.setCategory(category);
            translation.setId(getAvailableLanguages().get(translationPack.getTargetLanguage()), category);

            categoryTranslations.add(translation);
        }

        categoryTranslationRepository.saveAll(categoryTranslations);
        foodItemTranslationRepository.saveAll(foodTranslations);
        return true;
    }

    public TranslationPackDto getTranslationsForLanguage(String language) {
        if (!validateLanguage(language)) {
            throw new InvalidValueException("Language", language);
        }

        // Convert language name to language code for repository queries
        String languageCode = getAvailableLanguages().get(language);
        if (languageCode == null) {
            throw new InvalidValueException("Language code not found for language", language);
        }

        TranslationPackDto result = new TranslationPackDto();
        result.setTargetLanguage(language);
        result.setCategoryTranslations(new HashMap<>());
        result.setFoodItemTranslations(new HashMap<>());

        List<CategoryTranslation> categoryTranslations = categoryTranslationRepository.findByCategoryTranslationId_LanguageCode(languageCode);
        for (CategoryTranslation ct : categoryTranslations) {
            result.getCategoryTranslations().put(ct.getCategory().getCategoryName(), ct.getName());
        }

        List<FoodItemTranslation> foodTranslations = foodItemTranslationRepository.findByFoodItemTranslationId_LanguageCode(languageCode);
        for (FoodItemTranslation ft : foodTranslations) {
            result.getFoodItemTranslations().put(ft.getFoodItem().getFoodName(), List.of(ft.getName(), ft.getDescription()));
        }

        return result;
    }

    private boolean validateLanguage(String languageName) {
        // if it is in the supported languages list within JSON file in the resources folder
        List<String> availableLanguageNames = getAvailableLanguages().keySet().stream().toList();
        return availableLanguageNames.contains(languageName);
    }

    private Map<String, String> getAvailableLanguages() {
        if (cachedLanguages != null) {
            return cachedLanguages;
        }

        ClassPathResource resource = new ClassPathResource("languages.json");

        try (InputStream inputStream = resource.getInputStream()) {
            ObjectMapper mapper = new ObjectMapper();
            cachedLanguages = mapper.readValue(inputStream, new TypeReference<>() {
            });
        } catch (IOException e) {
            System.err.println("Failed to load supported languages: " + e.getMessage());
            e.printStackTrace();
            return Map.of();
        }

        return cachedLanguages;
    }

    private String getLanguageCode(String language) {
        if (getAvailableLanguages().containsKey(language)) {
            return getAvailableLanguages().get(language);
        }
        return null;
    }
}