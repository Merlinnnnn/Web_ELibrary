package com.spkt.libraSys.service.document;


import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;

public class DocumentException extends AppException {
    public DocumentException(ErrorCode errorCode) {
        super(errorCode);
    }

    public DocumentException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
}
