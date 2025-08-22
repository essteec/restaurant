package com.ste.restaurant;

import com.google.genai.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;

@EnableCaching
@SpringBootApplication
public class RestaurantApplication {

	public static void main(String[] args) {
		SpringApplication.run(RestaurantApplication.class, args);
	}

	@Bean  // update it from openai to gemini api
	public Client geminiClient(@Value("${gemini.apiKey}") String apiKey) {
		if (apiKey == null || apiKey.isEmpty()) {
			throw new IllegalArgumentException("Gemini API key must be provided");
		}
		return Client.builder()
				.apiKey(apiKey)
				.build();
	}
}
