package com.spkt.libraSys.service.loan;

import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;
import com.spkt.libraSys.service.document.PhysicalDocument.PhysicalDocumentEntity;
import com.spkt.libraSys.service.user.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoanValidationService {
    private final LoanRepository loanRepository;

    public void validateNewLoan(UserEntity user, PhysicalDocumentEntity physicalDoc) {
        validatePendingLoan(user, physicalDoc);
        validateBorrowLimit(user);
        validateBookAvailability(physicalDoc);
    }

    private void validatePendingLoan(UserEntity user, PhysicalDocumentEntity physicalDoc) {
        boolean isUserLoanPending = loanRepository.existsPendingLoanTransaction(user, physicalDoc, LoanStatus.RESERVED);
        if (isUserLoanPending) {
            throw new AppException(ErrorCode.RESOURCE_CONFLICT,
                    String.format("Bạn đã có yêu cầu mượn sách '%s' đang chờ phê duyệt.",
                            physicalDoc.getDocument().getDocumentName()));
        }
    }

    private void validateBorrowLimit(UserEntity user) {
        if (user.getCurrentBorrowedCount() + 1 > user.getMaxBorrowLimit()) {
            throw new AppException(ErrorCode.RESOURCE_CONFLICT,
                    "Bạn đã đạt giới hạn số lượng sách tối đa có thể mượn.");
        }
    }

    private void validateBookAvailability(PhysicalDocumentEntity physicalDoc) {
        if (physicalDoc.getAvailableCopies() <= 0) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                    "Sách hiện không có sẵn để mượn.");
        }
    }
}