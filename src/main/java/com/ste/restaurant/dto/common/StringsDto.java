package com.ste.restaurant.dto.common;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StringsDto {

    @NotEmpty(message = "At least one name must be provided")
    private Set<String> names;
}
