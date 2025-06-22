package com.spkt.libraSys.service.chatbot.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresAuth {
    String message() default "Vui lòng đăng nhập để sử dụng tính năng này";
} 