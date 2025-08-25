package com.ste.restaurant.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "category_translations")
public class CategoryTranslation {

    @Id
    private String languageCode;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
}
