package com.seastar.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by osx on 16/12/10.
 */
@ControllerAdvice
public class ExceptionHandlingController {

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public String handleError(Exception ex) {
        return "{\"error\":\"exception\", \"error_description\":\"" + ex.getMessage() + "\"}";
    }
}
