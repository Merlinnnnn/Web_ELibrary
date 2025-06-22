package com.spkt.libraSys.service.user;

import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;

public class UserException extends AppException {
    public UserException(ErrorCode errorCode) {
        super(errorCode);
    }

    public UserException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
}
