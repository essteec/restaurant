package com.ste.restaurant.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Profile("!test")
public class FrontendController {

    /**
     * Forwards requests to the single-page application's entry point (index.html).
     * This is a catch-all for paths that are not handled by other controllers (e.g., /api)
     * and do not appear to be static files (i.e., the final path segment does not contain a dot '.').
     */
    @RequestMapping(value = {"/", "/**/{path:[^\\.]*}"})
    public String forward() {
        return "forward:/index.html";
    }
}