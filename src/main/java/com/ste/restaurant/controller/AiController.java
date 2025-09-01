package com.ste.restaurant.controller;

import com.ste.restaurant.dto.TranslationPackDto;
import com.ste.restaurant.dto.common.StringDto;
import com.ste.restaurant.service.AiService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


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
    public String getFoodDescription(@RequestParam() @Size(min = 3) @NotBlank String foodName) {
        return aiService.getFoodDescriptionFromAi(foodName.toLowerCase());
    }

    @GetMapping("/translate/{language}")
    public TranslationPackDto getTranslations(@PathVariable String language) {
        return aiService.getTranslationsForLanguage(language);
    }

    @PostMapping("/translate")  // not saving to the db
    public TranslationPackDto translateAllTo(@RequestBody @Valid StringDto language) {
        return aiService.translateAllTo(language.getName());
    }

    @PostMapping("/translate/finalize")  // after user previewed and changed the translations, it request with
    public ResponseEntity<Boolean> finalizeTranslations(@RequestBody TranslationPackDto translationPack) {
        boolean created = aiService.finalizeTranslations(translationPack);
        return created
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }
}
