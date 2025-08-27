package com.ste.restaurant.entity;

import com.ste.restaurant.entity.id.CategoryTranslationId;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
@Table(name = "category_translations")
public class CategoryTranslation {

    @EmbeddedId
    private CategoryTranslationId categoryTranslationId;

    private String name;

    @MapsId("categoryId")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    public void setId(String languageCode, Category category) {
        CategoryTranslationId id = new CategoryTranslationId();
        id.setLanguageCode(languageCode);
        id.setCategoryId(category.getCategoryId());
        this.setCategoryTranslationId(id);
        this.setCategory(category);
    }
}
