package com.spkt.libraSys.service.qrcode;

import com.spkt.libraSys.service.loan.LoanStatus;

public class JwtTokenData {
    private Long transactionId;
    private LoanStatus status;

    public JwtTokenData(Long transactionId, LoanStatus status) {
        this.transactionId = transactionId;
        this.status = status;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public LoanStatus getStatus() {
        return status;
    }
}
