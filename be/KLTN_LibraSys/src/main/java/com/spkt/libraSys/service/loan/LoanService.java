package com.spkt.libraSys.service.loan;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for managing book loan operations in the library system.
 * Provides methods for creating, retrieving, and managing loan transactions,
 * including QR code handling and automated loan management features.
 */
public interface LoanService {
    /**
     * Creates a new loan transaction.
     * @param loanRequest Request containing loan details
     * @return Response containing the created loan information
     */
    LoanResponse createLoan(LoanRequest loanRequest);

    /**
     * Retrieves all loans for a specific user with pagination.
     * @param userId ID of the user
     * @param pageable Pagination information
     * @return Page of loan responses
     */
    Page<LoanResponse> getUserLoans(String userId, Pageable pageable);

    /**
     * Retrieves a loan transaction by its ID.
     * @param loanId ID of the loan transaction
     * @return Loan response containing the loan details
     */
    LoanResponse getLoanId(Long loanId);

    /**
     * Retrieves all loan transactions with pagination.
     * @param pageable Pagination information
     * @return Page of loan responses
     */
    Page<LoanResponse> getAll(Pageable pageable);

    /**
     * Checks if a user is currently borrowing a specific physical document.
     * @param physicalDocId ID of the physical document
     * @return true if the user is borrowing the document, false otherwise
     */
    boolean isUserBorrowingPhysicalDoc(Long physicalDocId);

    /**
     * Creates a fine for a damaged or lost book.
     * @param loanId ID of the loan transaction
     * @return Loan response containing the updated loan information
     */
    LoanResponse createFineForDamagedOrLostBook(Long loanId);

    /**
     * Generates a QR code for a loan transaction.
     * @param transactionId ID of the loan transaction
     * @return Byte array containing the QR code image
     */
    byte[] generateQRCode(Long transactionId);

    /**
     * Handles QR code scanning for loan transactions.
     * @param action Action to perform
     * @param qrToken QR code token
     * @return Loan response containing the updated loan information
     */
    LoanResponse handleQrcodeScan(String action, String qrToken);

    /**
     * Automatically cancels expired reservations.
     */
    void autoCancelExpiredReservations();

    /**
     * Sends notifications to users whose loans are nearing the due date.
     */
    void notifyUsersNearDueDate();

    /**
     * Processes a cash payment for a loan transaction.
     * @param loanId ID of the loan transaction
     * @return Loan response containing the updated loan information
     */
    LoanResponse processCashPayment(Long loanId);

    /**
     * Blocks users who have had books overdue for more than 30 days.
     */
    void blockUsersWithReturnOverdue30Days();

    /**
     * Retrieves loan transactions that are either borrowed or have unpaid fines.
     * @param pageable Pagination information
     * @return Page of loan responses
     */
    Page<LoanResponse> getUserLoansWithStatusOrPaymentStatus(Pageable pageable);
}
