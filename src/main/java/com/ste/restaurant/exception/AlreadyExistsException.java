package com.ste.restaurant.exception;

import java.util.List;

public class AlreadyExistsException extends CustomException {

    public AlreadyExistsException(String entity, String name) {
        super(entity + " already exists",
                entity.toUpperCase() + "_ALREADY_EXISTS",
                409,
                "A " + entity + " with name '" + name + "' already exists"
        );
    }

    public AlreadyExistsException(String entity, Long id) {
        super(entity + " already exists",
                entity.toUpperCase() + "_ALREADY_EXISTS",
                409,
                "A " + entity + " with id '" + id + "' already exists"
        );
    }
}