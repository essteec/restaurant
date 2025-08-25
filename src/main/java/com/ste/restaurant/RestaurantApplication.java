package com.ste.restaurant;

import com.google.genai.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@EnableCaching
@SpringBootApplication
public class RestaurantApplication {

	public static void main(String[] args) {
		SpringApplication.run(RestaurantApplication.class, args);
	}

	@Bean  // update it from openai to gemini api
	@Profile("!test") // Don't create this bean when test profile is active
	public Client geminiClient(@Value("${gemini.apiKey}") String apiKey) {
		if (apiKey == null || apiKey.isEmpty()) {
			throw new IllegalArgumentException("Gemini API key must be provided");
		}
		return Client.builder()
				.apiKey(apiKey)
				.build();
	}
}
