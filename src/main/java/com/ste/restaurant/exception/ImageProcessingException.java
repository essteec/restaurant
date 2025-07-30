package com.ste.restaurant.exception;

import java.util.List;

public class ImageProcessingException extends CustomException {

    public ImageProcessingException(String message) {
        super("Image processing error: " + message,
                "IMAGE_PROCESSING_ERROR",
                500,
                message
        );
    }
} 