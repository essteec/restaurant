package com.ste.restaurant.exception;

import java.util.List;

public class NullValueException extends CustomException {

    public NullValueException(String entity, String fieldName) {
        super(entity + " " + fieldName + " cannot be null",
                entity.toUpperCase() + "_" + fieldName.toUpperCase() + "_NULL",
                400,
                "The " + fieldName + " field for " + entity + " must not be null"
        );
    }

//    not used
//    public NullValueException(String fieldName) {
//        super(fieldName + " cannot be null",
//                fieldName.toUpperCase() + "_NULL",
//                400,
//                List.of("The " + fieldName + " field must not be null"));
//    }
} 