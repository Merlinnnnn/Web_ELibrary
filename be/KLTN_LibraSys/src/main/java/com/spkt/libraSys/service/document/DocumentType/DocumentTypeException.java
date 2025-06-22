package com.spkt.libraSys.service.document.DocumentType;

import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;

public class DocumentTypeException extends AppException {
    public DocumentTypeException(ErrorCode errorCode) {
        super(errorCode);
    }

    public DocumentTypeException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
}
