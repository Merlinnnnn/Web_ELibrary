package com.spkt.libraSys.service.document.course;

import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;

public class CourseException extends AppException {
    public CourseException(ErrorCode errorCode) {
        super(errorCode);
    }

    public CourseException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
}
