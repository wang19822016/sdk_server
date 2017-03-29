package com.seastar.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by osx on 16/12/10.
 */
@ControllerAdvice
public class ExceptionHandlingController {

    @ExceptionHandler(Exception.class)
    public void handleError(HttpServletRequest req, HttpServletResponse rsp, Exception ex) {
        try {
            String body = "{\"error\":\"exception\", \"error_description\":\"" + ex.getMessage() + "\"}";

            rsp.setHeader("content-type", "application/json;charset=UTF-8");//通过设置响应头控制浏览器以UTF-8的编码显示数据，如果不加这句话，那么浏览器显示的将是乱码
            rsp.setStatus(HttpStatus.FORBIDDEN.value());
            rsp.getWriter().write(body);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
