package com.spkt.libraSys.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppException extends RuntimeException {

    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.messageCustom = null;
    }
    // Constructor mới để hỗ trợ thông điệp tùy chỉnh
    public AppException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.messageCustom = customMessage;
        this.errorCode = errorCode;
    }

    private ErrorCode errorCode;
    private String messageCustom;

}
