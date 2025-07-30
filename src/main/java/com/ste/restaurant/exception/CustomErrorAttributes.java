package com.ste.restaurant.exception;


import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class CustomErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        Map<String, Object> errorAttr = super.getErrorAttributes(webRequest, options);
        Throwable error = getError(webRequest);

        List<String> messages = new ArrayList<>();

        messages.add(error.getMessage());
        errorAttr.remove("message");
        errorAttr.put("messages", messages);

        return errorAttr;
    }
}
