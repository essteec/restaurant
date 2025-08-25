package com.ste.restaurant;

import com.google.genai.Client;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestConfig {
    
    @Bean
    @Profile("test") // Only create this bean when test profile is active
    public Client geminiClient() {
        // Return a mock Gemini client for tests to avoid requiring API key
        return mock(Client.class);
    }
}
