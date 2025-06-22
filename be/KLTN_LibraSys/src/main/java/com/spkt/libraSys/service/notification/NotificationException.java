package com.spkt.libraSys.service.notification;

import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;

public class NotificationException extends AppException {
    public NotificationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public NotificationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}