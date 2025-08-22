package com.ste.restaurant.service;

import org.springframework.stereotype.Service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.ste.restaurant.exception.InvalidValueException;
import org.springframework.cache.annotation.Cacheable;

@Service
public class AiService {

    private final Client geminiClient;

    public AiService(Client geminiClient) {
        this.geminiClient = geminiClient;
    }

    /**
     * Generates a concise menu description for the given food name using Gemini.
     * @param foodName The food name.
     * @return JSON string with the description or empty JSON object if failed.
     */
    @Cacheable("foodDescriptions")
    public String getFoodDescriptionFromAi(String foodName) {

        String prompt = """
        You are a professional restaurant menu writer.
        Given the food name, generate a concise menu description.
        Return ONLY the description text - no JSON, no quotes, no extra formatting.
        
        Guidelines:
        - Keep it concise (8-25 words)
        - Highlight key ingredients and prominent flavors
        - Make it appetizing and descriptive
        - Use proper grammar and punctuation
        - Do not include prices, cooking instructions, or promotional language
        - If you cannot generate a description, return "No description available"
        
        Food name: "%s"
        
        Description:
            """.formatted(foodName);

        GenerateContentResponse response;
        try {
            response = geminiClient.models.generateContent("gemini-2.0-flash-lite", prompt, null);
            System.out.println("AI response: " + response.text());

        } catch (Exception e) {
            System.out.println("«ERROR: ");
            e.printStackTrace();
            System.out.println("ERROR»\n");
            throw new InvalidValueException("AI response", "Food name", foodName + " «-_-» " + e.getMessage());
        }

        String description = response.text().trim();

        if (description.equals("No description available") || description.isEmpty()) {
            throw new InvalidValueException("Food item", "Food name", foodName);
        }

        return description;
    }
}
