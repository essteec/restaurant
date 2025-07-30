package com.ste.restaurant.exception;

import java.util.List;

public class InvalidOperationException extends CustomException {

//    public InvalidOperationException(String message) {
//        super(message, "INVALID_OPERATION", 400, List.of(message));
//    }

    public InvalidOperationException(String entity, String operation) {
        super("Invalid operation on " + entity + ": " + operation,
                entity.toUpperCase() + "_INVALID_OPERATION",
                400,
                "Operation '" + operation + "' is not allowed on " + entity
        );
    }
} 