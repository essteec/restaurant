package com.ste.restaurant.dto;

import com.ste.restaurant.entity.RequestType;
import com.ste.restaurant.entity.TableTop;
import com.ste.restaurant.entity.User;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CallRequestDtoBasic {
    private RequestType type;

    private String message;
}
