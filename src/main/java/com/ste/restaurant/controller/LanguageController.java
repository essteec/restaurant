package com.ste.restaurant.controller;

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

    @GetMapping
    public long getUniqueLanguageCodeCount() {
        return languageService.countDistinctLanguages();
    }
}
