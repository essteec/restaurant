package com.ste.restaurant.exception;

import java.util.List;

public class InvalidValueException extends CustomException {

    public InvalidValueException(String entity, String field, String value) {
        super("Invalid value for " + entity + " " + field + ": '" + value + "'",
                entity.toUpperCase() + "_" + field.toUpperCase() + "_INVALID",
                400,
                "'" + value + "' is not a valid value for " + field + " in " + entity
        );
    }

//    public InvalidValueException(String field, String value) {
//        super("Invalid value for " + field + ": '" + value + "'",
//                field.toUpperCase() + "_INVALID",
//                400,
//                List.of("'" + value + "' is not a valid value for " + field));
//    }
}
