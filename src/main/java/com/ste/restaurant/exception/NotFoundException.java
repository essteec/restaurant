package com.ste.restaurant.exception;


import java.util.List;

public class NotFoundException extends CustomException {

    public NotFoundException(String entity, String name) {
        super(entity + " not found",
                entity.toUpperCase() + "_NOT_FOUND",
                404,
                "No " + entity + " exists with: " + name
        );
    }

    public NotFoundException(String entity, Long id) {
        super(entity + " not found",
                entity.toUpperCase() + "_NOT_FOUND",
                404,
                "No " + entity + " exists with id " + id
        );
    }

    public NotFoundException(String entity) {
        super(entity + " not found",
                entity.toUpperCase() + "_NOT_FOUND",
                404,
                "No " + entity + " exists"
        );
    }
}