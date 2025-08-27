package com.ste.restaurant.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ste.restaurant.service.LanguageService;

@RequestMapping("/rest/api/languages")
@RestController
public class LanguageController {

    LanguageService languageService;

    public LanguageController (LanguageService languageService) {
        this.languageService = languageService;
    }

    @GetMapping("/supported/count")
    public long getUniqueLanguageCodeCount() {
        return languageService.countDistinctLanguages();
    }

    @GetMapping("/supported")
    public List<String> getSupportedLanguages() {
        return languageService.getSupportedLanguages();
    }
}
