package com.spkt.libraSys.config;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

@ControllerAdvice
public class GlobalStringTrimAdvice {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // Tự động trim các chuỗi và chuyển "" thành null
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }
}
