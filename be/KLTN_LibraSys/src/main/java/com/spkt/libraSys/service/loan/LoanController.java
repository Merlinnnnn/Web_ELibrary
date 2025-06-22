package com.spkt.libraSys.service.loan;

import com.spkt.libraSys.exception.ApiResponse;
import com.spkt.libraSys.service.PageDTO;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller class handling book loan transaction requests.
 * Provides endpoints for creating, updating, retrieving, and managing book loan transactions.
 */
@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LoanController {

    LoanService loanService;

    /**
     * Creates a new loan transaction.
     * @param request Loan request containing transaction details
     * @return ResponseEntity containing the created loan transaction
     */
    @PostMapping
    public ResponseEntity<ApiResponse<LoanResponse>> createLoanTransaction(@RequestBody LoanRequest request) {
        LoanResponse response = loanService.createLoan(request);
        ApiResponse<LoanResponse> apiResponse = ApiResponse.<LoanResponse>builder()
                .message("Yêu cầu mượn sách đã được tạo thành công")
                .data(response)
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Retrieves a loan transaction by its ID.
     * @param id ID of the loan transaction to retrieve
     * @return ResponseEntity containing the loan transaction details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LoanResponse>> getLoanTransactionById(@PathVariable Long id) {
        LoanResponse response = loanService.getLoanId(id);
        ApiResponse<LoanResponse> apiResponse = ApiResponse.<LoanResponse>builder()
                .message("Thông tin giao dịch được lấy thành công")
                .data(response)
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Retrieves all loan transactions with pagination.
     * Requires ADMIN or MANAGER role.
     * @param pageable Pagination information
     * @return ResponseEntity containing the paginated list of loan transactions
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<PageDTO<LoanResponse>>> getAllLoanTransactions(
            Pageable pageable) {
        Page<LoanResponse> response = loanService.getAll(pageable);
        ApiResponse<PageDTO<LoanResponse>> apiResponse = ApiResponse.<PageDTO<LoanResponse>>builder()
                .message("Tất cả giao dịch được lấy thành công")
                .data(new PageDTO<>(response))
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Checks if a user is currently borrowing a specific physical document.
     * @param physicalId ID of the physical document to check
     * @return ResponseEntity containing the borrowing status
     */
    @GetMapping("/user/check-user-borrowing/{physicalId}")
    public ResponseEntity<ApiResponse<Boolean>> checkUserBorrowingDocument(@PathVariable Long physicalId) {
        boolean isBorrowing = loanService.isUserBorrowingPhysicalDoc(physicalId);
        ApiResponse<Boolean> apiResponse = ApiResponse.<Boolean>builder()
                .message(isBorrowing ? "Người dùng đang mượn cuốn sách này" : "Người dùng không mượn cuốn sách này")
                .data(isBorrowing)
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Retrieves all books borrowed by a specific user with pagination.
     * @param userId ID of the user
     * @param page Page number
     * @param size Page size
     * @return ResponseEntity containing the paginated list of borrowed books
     */
    @GetMapping("/user/borrowed-books")
    public ResponseEntity<ApiResponse<PageDTO<LoanResponse>>> getUserBorrowedBooks(
            @RequestParam String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("loanDate").descending());

        Page<LoanResponse> borrowedBooks = loanService.getUserLoans(userId, pageable);

        ApiResponse<PageDTO<LoanResponse>> apiResponse = ApiResponse.<PageDTO<LoanResponse>>builder()
                .message("Danh sách các sách đang mượn của người dùng")
                .data(new PageDTO<>(borrowedBooks))
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Handles barcode scanning for loan transactions.
     * @param action Action to perform
     * @param token Token for authentication
     * @return ResponseEntity containing the updated loan transaction
     */
    @GetMapping("/scan")
    public ResponseEntity<ApiResponse<LoanResponse>> handleBarcodeScan(@RequestParam String action,@RequestParam  String token) {
        LoanResponse response = loanService.handleQrcodeScan(action,token);
        ApiResponse<LoanResponse> apiResponse = ApiResponse.<LoanResponse>builder()
                .message("Giao dịch được cập nhật thành công dựa trên quét mã vạch")
                .data(response)
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Generates and retrieves a QR code image for a loan transaction.
     * @param transactionId ID of the loan transaction
     * @return ResponseEntity containing the QR code image
     */
    @GetMapping("/{transactionId}/qrcode-image")
    public ResponseEntity<byte[]> getBarcodeImage(@PathVariable Long transactionId) {
        byte[] qrCodeImage = loanService.generateQRCode(transactionId);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(qrCodeImage);
    }

    /**
     * Creates a fine for a damaged or lost book.
     * Requires ADMIN or MANAGER role.
     * @param loanId ID of the loan transaction
     * @return ResponseEntity containing the updated loan transaction
     */
    @PostMapping("/{loanId}/fine")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<LoanResponse>> createFineForDamagedOrLostBook(
            @PathVariable Long loanId) {
        return ResponseEntity.ok(ApiResponse.<LoanResponse>builder().
                message("Thanh Toan tien mat thanh cong").
                data(loanService.createFineForDamagedOrLostBook(loanId))
                .build());
    }

    /**
     * Processes a cash payment for a loan transaction.
     * Requires ADMIN or MANAGER role.
     * @param loanId ID of the loan transaction
     * @return ResponseEntity containing the updated loan transaction
     */
    @PostMapping("/{loanId}/paymentcash")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<LoanResponse>> paymentByCash(
            @PathVariable Long loanId) {
        return ResponseEntity.ok(ApiResponse.<LoanResponse>builder().
                message("Thanh Toan tien mat thanh cong").
                data(loanService.processCashPayment(loanId))
                .build());
    }

    /**
     * Retrieves loan transactions that are either borrowed or unpaid.
     * @param pageable Pagination information
     * @return ResponseEntity containing the paginated list of loan transactions
     */
    @GetMapping("/borrowed-or-unpaid")
    public ResponseEntity<ApiResponse<PageDTO<LoanResponse>>> getUserLoansWithStatusOrPaymentStatus(Pageable pageable) {
        Page<LoanResponse> loans = loanService.getUserLoansWithStatusOrPaymentStatus(pageable);
        ApiResponse<PageDTO<LoanResponse>> apiResponse = ApiResponse.<PageDTO<LoanResponse>>builder()
                .message("Danh sách sách đang mượn hoặc chưa thanh toán")
                .data(new PageDTO<>(loans))
                .build();

        return ResponseEntity.ok(apiResponse);
    }
}
