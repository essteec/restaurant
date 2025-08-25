package com.ste.restaurant.controller;

import com.ste.restaurant.service.AiService;

import jakarta.validation.constraints.Size;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Profile;

@RestController
@RequestMapping("/rest/api/ai")
@PreAuthorize("hasRole('ADMIN')")
@Profile("!test")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/description")
    public String getFoodDescription(@RequestParam() @Size(min = 5, message = "foodName must be at least 5 characters long") String foodName) {
        return aiService.getFoodDescriptionFromAi(foodName.toLowerCase());
    }
}
