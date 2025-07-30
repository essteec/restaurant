package com.ste.restaurant.exception;

import java.util.List;

public class AlreadyHasException extends CustomException {

    public AlreadyHasException(String entity, String field, String value) {
        super(entity + " already has " + field + " as " + value,
                entity.toUpperCase() + "_ALREADY_HAS_" + value.toUpperCase(),
                409,
                entity + " already has " + value + " in field: " + field
        );
    }

//    public AlreadyHasException(String entity, String value) {
//        super(entity + " already has " + value,
//                entity.toUpperCase() + "_ALREADY_HAS_" + value.toUpperCase(),
//                409,
//                List.of(entity + " already has " + value));
//    }
} 