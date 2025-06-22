package com.spkt.libraSys.service.loan;

import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;
import com.spkt.libraSys.service.access.AuthService;
import com.spkt.libraSys.service.document.DocumentEntity;
import com.spkt.libraSys.service.document.DocumentRepository;
import com.spkt.libraSys.service.document.PhysicalDocument.PhysicalDocumentEntity;
import com.spkt.libraSys.service.document.PhysicalDocument.PhysicalDocumentRepository;
import com.spkt.libraSys.service.email.EmailService;
import com.spkt.libraSys.service.loan.*;
import com.spkt.libraSys.service.notification.*;
import com.spkt.libraSys.service.qrcode.JwtTokenData;
import com.spkt.libraSys.service.qrcode.QRService;
import com.spkt.libraSys.service.role.RoleEntity;
import com.spkt.libraSys.service.user.UserEntity;
import com.spkt.libraSys.service.user.UserRepository;
import com.spkt.libraSys.service.user.UserStatus;
import com.spkt.libraSys.service.webSocket.WebSocketService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.swing.text.Document;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Implementation of the LoanService interface.
 * Handles all loan-related operations including creation, management, and automated processes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LoanServiceImpl implements LoanService {
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String MANAGER_ROLE = "MANAGER";
    private static final int NOTIFICATION_DAYS_BEFORE_DUE = 3;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    
    private final AuthService authService;
    private final PhysicalDocumentRepository physicalDocumentRepository;
    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final LoanMapper loanMapper;
    private final QRService qrService;
    private final LoanValidationService validationService;
    private final NotificationService notificationService;
    private final WebSocketService webSocketService;
    private final EmailService emailService;

    @Value("${app.loan.duration.days}")
    @NonFinal
    private int loanDurationDays;
    
    @Value("${app.chatbot.url}")
    @NonFinal
    private String urlPublic;

    @Override
    @Transactional
    public LoanResponse createLoan(LoanRequest request) {
        log.info("Creating new loan request for physical document ID: {}", request.getPhysicalDocId());

        UserEntity user = authService.getCurrentUser();
        // Check if user is already borrowing this document

        boolean hasOverdueLoan = loanRepository.existsByUserEntityAndReturnDateIsNullAndDueDateBefore(user, LocalDate.now());
        boolean hasUnpaidFine = loanRepository.existsByUserEntityAndPaymentStatus(user, LoanEntity.PaymentStatus.UNPAID);

        if (hasOverdueLoan || hasUnpaidFine) {
            throw new AppException(ErrorCode.USER_HAS_OVERDUE_LOAN,
                    "Bạn đang có tài liệu quá hạn chưa trả hoặc khoản phạt chưa thanh toán, không thể mượn thêm.");
        }

        PhysicalDocumentEntity physicalDoc = getPhysicalDocument(request.getPhysicalDocId());
        boolean isAlreadyBorrowed = loanRepository.existsByUserEntityAndPhysicalDocAndStatusIn(
                user,
                physicalDoc.getPhysicalDocumentId(),
                Set.of(LoanStatus.BORROWED, LoanStatus.RESERVED)
        );
        if (isAlreadyBorrowed) {
            throw new AppException(ErrorCode.DUPLICATE_DOCUMENT,
                    "Bạn đã mượn tài liệu này hoặc đang chờ xác nhận.");
        }

        validationService.validateNewLoan(user, physicalDoc);

        LoanEntity loan = createAndSaveLoan(user, physicalDoc);
        updatePhysicalDocumentAvailability(physicalDoc, true);
        sendLoanNotification(loan, NotificationType.LOAN_CREATED);

        log.info("Loan request created successfully with ID: {}", loan.getTransactionId());
        return loanMapper.toLoanTransactionResponse(loan);
    }

    @Override
    @Transactional
    public LoanResponse handleQrcodeScan(String action, String qrToken) {
        log.info("Processing QR code scan with action: {}", action);
        
        // 1. Get scanner information (librarian)
        UserEntity scanner = authService.getCurrentUser();
        
        // 2. Check librarian permissions
        if (!hasAdminOrManagerRole(scanner)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS,
                "Chỉ thủ thư mới có quyền quét QR code");
        }

        try {
            // 3. Parse and validate JWT token
            JwtTokenData tokenData = validateQrToken(qrToken);
            LoanEntity loan = getLoanAndValidateStatus(tokenData);
            
            // 4. Check if action is valid for current status
            validateActionForStatus(action, loan.getStatus());
            PhysicalDocumentEntity physicalDoc = loan.getPhysicalDoc();
            // 5. Process status change
            switch (loan.getStatus()) {
                case RESERVED -> {
                    // Change from RESERVED to BORROWED
                    loan.setStatus(LoanStatus.BORROWED);
                    loan.setDueDate(LocalDate.now().plusDays(loanDurationDays));
                    loan.setLoanDate(LocalDateTime.now());
                    loan.setLibrarianName(scanner.getUsername());
                    // Update available book count
                    physicalDocumentRepository.save(loan.getPhysicalDoc());
                    // Send notification to borrower
                    sendLoanNotification(loan, NotificationType.LOAN_APPROVED);
                }
                case BORROWED -> {
                    // Change from BORROWED to RETURNED
                    loan.setStatus(LoanStatus.RETURNED);
                    loan.setReturnDate(LocalDateTime.now());
                    loan.setLibrarianName(scanner.getUsername());
                    // Check for overdue
                    if(loan.isOverdue()){
                        loan.setReturnCondition(LoanEntity.Condition.OVERDUE);
                        // Calculate overdue days
                        long daysLate = ChronoUnit.DAYS.between(loan.getDueDate(), LocalDateTime.now());
                        loan.setFineAmount(Math.min(physicalDoc.getPrice() , daysLate * 5000));
                        loan.setPaymentStatus(LoanEntity.PaymentStatus.UNPAID);
                        // Notify user about fine
                        notificationService.createAndSendNotification(
                            loan.getUserEntity().getUserId(),
                            NotificationType.LOAN_FINE,
                            Map.of("fineAmount", loan.getFineAmount(), "loanId", loan.getTransactionId()),
                            String.valueOf(loan.getTransactionId()),
                            "LOAN"
                        );
                    }else{
                        loan.setReturnCondition(LoanEntity.Condition.NORMAL);
                    }
                    
                    // Update count
                    updatePhysicalDocumentAvailability(loan.getPhysicalDoc(), false);
                    UserEntity user = loan.getUserEntity();
                    user.setCurrentBorrowedCount(user.getCurrentBorrowedCount() - 1);
                    userRepository.save(user);
                    // Send notification to borrower
                    sendLoanNotification(loan, NotificationType.LOAN_RETURNED);
                }
                default -> throw new AppException(ErrorCode.INVALID_STATUS,
                    "Trạng thái giao dịch không hợp lệ");
            }

            // 6. Save transaction information
            loanRepository.save(loan);

            // 7. Send real-time update
            webSocketService.sendUpdateStatusLoan(
                loan.getUserEntity().getUserId(),
                loanMapper.toLoanTransactionResponse(loan)
            );

            log.info("QR code scanned by librarian {} for loan ID: {}", 
                scanner.getUserId(), loan.getTransactionId());
                
            return loanMapper.toLoanTransactionResponse(loan);
            
        } catch (AppException e) {
            log.error("Error processing QR code scan: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error processing QR code scan: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Error processing QR code scan");
        }
    }

    @Override
    public Page<LoanResponse> getUserLoans(String userId, Pageable pageable) {
        log.info("Fetching loans for user ID: {}", userId);

        UserEntity currentUser = authService.getCurrentUser();
        validateUserAccess(currentUser, userId);

        Page<LoanEntity> loans = loanRepository.findByUserEntity_UserId(userId , pageable);
        return loans.map(loanMapper::toLoanTransactionResponse);
    }

    @Override
    public LoanResponse getLoanId(Long loanId) {
        return loanRepository.findById(loanId)
                .map(loanMapper::toLoanTransactionResponse)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND,
                        "Không tìm thấy giao dịch mượn sách"));
    }

    @Override
    public Page<LoanResponse> getAll(Pageable pageable) {
        return loanRepository.findAll(pageable)
                .map(loanMapper::toLoanTransactionResponse);
    }

    @Override
    public boolean isUserBorrowingPhysicalDoc(Long physicalId) {
        UserEntity user = authService.getCurrentUser();
        return loanRepository.existsByUserEntityAndPhysicalDocAndStatusIn(
                user,
                physicalId,
                Set.of(LoanStatus.BORROWED, LoanStatus.RESERVED)
        );
    }

    @Override
    public byte[] generateQRCode(Long transactionId) {
        UserEntity currentUser = authService.getCurrentUser();
        LoanEntity loan = validateAndGetLoan(transactionId, currentUser);

        String token = qrService.generateJwtTokenLoan(transactionId, loan.getStatus());
        
        String fullUrl =  "action=" + loan.getStatus().toString().toLowerCase() + "&token=" + token;

        return qrService.generateQRCode(fullUrl);
    }

    @Override
    @Transactional
    public void autoCancelExpiredReservations() {
        log.info("Starting auto-cancel process for expired RESERVED loans");
        LocalDateTime startTime = LocalDateTime.now();

        List<LoanEntity> expiredLoans = loanRepository.findByStatus(LoanStatus.RESERVED);
        log.info("Found {} expired loans to process", expiredLoans.size());

        if (expiredLoans.isEmpty()) {
            log.info("No expired loans to process");
            return;
        }

        List<PhysicalDocumentEntity> updatedDocs = new ArrayList<>();
        List<LoanEntity> updatedLoans = new ArrayList<>();

        for (LoanEntity loan : expiredLoans) {
            try {
                // Update loan status
                loan.setStatus(LoanStatus.CANCELLED_AUTO);
                updatedLoans.add(loan);

                // Update document availability
                PhysicalDocumentEntity doc = loan.getPhysicalDoc();
                doc.setUnavailableCount(doc.getUnavailableCount() - 1);
                updatedDocs.add(doc);
                UserEntity user = loan.getUserEntity();
                user.setCurrentBorrowedCount(user.getCurrentBorrowedCount() - 1);
                updatedLoans.add(loan);
                // Prepare notification
                Map<String, Object> params = Map.of("bookName", doc.getDocument().getDocumentName());
                notificationService.createAndSendNotification(
                    loan.getUserEntity().getUserId(), 
                    NotificationType.LOAN_AUTO_CANCAL, 
                    params,
                    String.valueOf(loan.getTransactionId()),
                    "LOAN"
                );

                // Send real-time update
                webSocketService.sendUpdateStatusLoan(
                    loan.getUserEntity().getUserId(), 
                    loanMapper.toLoanTransactionResponse(loan)
                );

            } catch (Exception e) {
                log.error("Error processing loan ID {}: {}", loan.getTransactionId(), e.getMessage(), e);
            }
        }

        // Batch save all updates
        try {
            loanRepository.saveAll(updatedLoans);
            physicalDocumentRepository.saveAll(updatedDocs);
            
            LocalDateTime endTime = LocalDateTime.now();
            log.info("Auto-cancel process completed. Processed {} loans in {} seconds", 
                updatedLoans.size(),
                java.time.Duration.between(startTime, endTime).getSeconds()
            );
        } catch (Exception e) {
            log.error("Error saving batch updates: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Error processing expired loans");
        }
    }

    @Override
    @Transactional
    public void notifyUsersNearDueDate() {
        log.info("Starting notification process for loans due in {} days", NOTIFICATION_DAYS_BEFORE_DUE);
        LocalDateTime startTime = LocalDateTime.now();

        LocalDate today = LocalDate.now();
        LocalDate dueDateThreshold = today.plusDays(NOTIFICATION_DAYS_BEFORE_DUE);

        List<LoanEntity> loans = loanRepository.findByStatusAndDueDateBetween(
            LoanStatus.BORROWED,
            today,
            dueDateThreshold
        );

        log.info("Found {} loans due within {} days", loans.size(), NOTIFICATION_DAYS_BEFORE_DUE);

        if (loans.isEmpty()) {
            log.info("No loans due for notification");
            return;
        }

        int successCount = 0;
        int failureCount = 0;

        for (LoanEntity loan : loans) {
            try {
                Map<String, Object> params = Map.of(
                    "bookName", loan.getPhysicalDoc().getDocument().getDocumentName(),
                    "dueDate", loan.getDueDate().format(DATE_FORMATTER)
                );

                notificationService.createAndSendNotification(
                    loan.getUserEntity().getUserId(),
                    NotificationType.LOAN_NEAR_DUE,
                    params,
                    String.valueOf(loan.getTransactionId()),
                    "LOAN"
                );
                successCount++;
            } catch (Exception e) {
                log.error("Failed to send notification for loan ID {}: {}", 
                    loan.getTransactionId(), e.getMessage(), e);
                failureCount++;
            }
        }

        LocalDateTime endTime = LocalDateTime.now();
        log.info("Notification process completed in {} seconds. Success: {}, Failed: {}", 
            java.time.Duration.between(startTime, endTime).getSeconds(),
            successCount,
            failureCount
        );
    }

    // Private helper methods
    private PhysicalDocumentEntity getPhysicalDocument(Long docId) {
        return physicalDocumentRepository.findById(docId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND,
                        "Không tìm thấy tài liệu"));
    }

    private LoanEntity createAndSaveLoan(UserEntity user, PhysicalDocumentEntity physicalDoc) {
        LoanEntity loan = LoanEntity.builder()
                .physicalDoc(physicalDoc)
                .userEntity(user)
                .status(LoanStatus.RESERVED)
                .loanDate(LocalDateTime.now())
                .build();
        user.setCurrentBorrowedCount(user.getCurrentBorrowedCount() + 1);
        userRepository.save(user);
        return loanRepository.save(loan);
    }

    private void updatePhysicalDocumentAvailability(PhysicalDocumentEntity physicalDoc, boolean isNewLoan) {
        int delta = isNewLoan ? 1 : -1;
        physicalDoc.setBorrowedCount(physicalDoc.getBorrowedCount() + delta);
        physicalDocumentRepository.save(physicalDoc);
    }

    private void sendLoanNotification(LoanEntity loan, NotificationType type) {
        Map<String, Object> params = new HashMap<>();
        params.put("bookName", loan.getPhysicalDoc().getDocument().getDocumentName());

        if (loan.getDueDate() != null) {
            params.put("dueDate", loan.getDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }

        notificationService.createAndSendNotification(
                loan.getUserEntity().getUserId(),
                type,
                params,
                String.valueOf(loan.getTransactionId()),
                "LOAN"
        );

        webSocketService.sendUpdateStatusLoan(
                loan.getUserEntity().getUserId(),
                loanMapper.toLoanTransactionResponse(loan)
        );
    }

    private JwtTokenData validateQrToken(String qrToken) {
        JwtTokenData data = qrService.parseJwtToken(qrToken);
        if (data.getTransactionId() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Mã QR không hợp lệ hoặc đã hết hạn");
        }
        return data;
    }

    private LoanEntity getLoanAndValidateStatus(JwtTokenData tokenData) {
        LoanEntity loan = loanRepository.findById(tokenData.getTransactionId())
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND,
                        "Không tìm thấy giao dịch mượn sách"));

        if (!loan.getStatus().equals(tokenData.getStatus())) {
            throw new AppException(ErrorCode.INVALID_STATUS,
                    "Trạng thái giao dịch không hợp lệ");
        }

        return loan;
    }

    private LoanEntity validateAndGetLoan(Long transactionId, UserEntity currentUser) {
        LoanEntity loan = loanRepository.findById(transactionId)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND,
                        "Không tìm thấy giao dịch mượn sách"));

        if (!currentUser.getUserId().equals(loan.getUserEntity().getUserId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS,
                    "Bạn không có quyền truy cập giao dịch này");
        }

        return loan;
    }

    private void validateUserAccess(UserEntity currentUser, String requestedUserId) {
        boolean isAdmin = hasAdminOrManagerRole(currentUser);
        boolean isOwner = currentUser.getUserId().equals(requestedUserId);

        if (!isAdmin && !isOwner) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền truy cập");
        }
    }

    private boolean hasAdminOrManagerRole(UserEntity user) {
        return user.getRoleEntities().stream()
                .map(RoleEntity::getRoleName)
                .anyMatch(role -> role.equals("ADMIN") || role.equals("MANAGER"));
    }

    private void validateActionForStatus(String action, LoanStatus currentStatus) {
        String expectedAction = currentStatus.toString().toLowerCase();
        if (!action.equals(expectedAction)) {
            throw new AppException(ErrorCode.INVALID_STATUS,
                String.format("Hành động '%s' không hợp lệ với trạng thái hiện tại '%s'", 
                    action, currentStatus));
        }
    }

    @Override
    public LoanResponse createFineForDamagedOrLostBook(Long loanId) {
        // 1. Find loan record
        LoanEntity loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND,
                    "Không tìm thấy giao dịch mượn sách"));

        // 2. Check status: 1.User hasn't borrowed 2.User has borrowed
        if (loan.getStatus() != LoanStatus.BORROWED && loan.getStatus() != LoanStatus.RETURNED) {
            throw new AppException(ErrorCode.TRANSACTION_NOT_FOUND,
                    "Giao dịch không hợp lệ");
        }
        if(loan.getReturnCondition().equals(LoanEntity.Condition.DAMAGED)){
            throw new AppException(ErrorCode.DUPLICATE_DOCUMENT,
                    "Ban da tao khoan phat ");
        }

        // 3. Calculate fine amount
        PhysicalDocumentEntity physicalDoc = loan.getPhysicalDoc();
        loan.setFineAmount(physicalDoc.getPrice());
        loan.setReturnCondition(LoanEntity.Condition.DAMAGED);
        loan.setPaymentStatus(LoanEntity.PaymentStatus.UNPAID);
        // Update damaged book count
        physicalDoc.setUnavailableCount(physicalDoc.getUnavailableCount() + 1);
        physicalDocumentRepository.save(physicalDoc);
        loanRepository.save(loan);
        // Notify user about fine
        notificationService.createAndSendNotification(
            loan.getUserEntity().getUserId(),
            NotificationType.LOAN_FINE,
            Map.of("fineAmount", loan.getFineAmount(), "loanId", loan.getTransactionId()),
                String.valueOf(loan.getTransactionId()),
                "LOAN"
        );
        return loanMapper.toLoanTransactionResponse(loan);
    }

    @Override
    @Transactional
    public LoanResponse processCashPayment(Long loanId) {
        // 1. Find loan record
        LoanEntity loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND,
                    "Không tìm thấy giao dịch mượn sách"));

        // 2. Check if there is a fine
        if (loan.getFineAmount() <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Không có khoản phạt nào cần thanh toán");
        }

        // 3. Check payment status
        if (loan.getPaymentStatus() != LoanEntity.PaymentStatus.UNPAID) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Khoản phạt đã được thanh toán");
        }

        // 4. Update payment status
        loan.setPaymentStatus(LoanEntity.PaymentStatus.CASH);
        loan.setPaidAt(LocalDateTime.now());
        // Librarian who received payment
        loan.setLibrarianName(authService.getCurrentUser().getUsername());
        // 5. Save changes
        loan = loanRepository.save(loan);

        // 6. Send notification
        notificationService.createAndSendNotification(
            loan.getUserEntity().getUserId(),
            NotificationType.LOAN_FINE,
            Map.of(
                "fineAmount", loan.getFineAmount(),
                "loanId", loan.getTransactionId()
            ),
                String.valueOf(loan.getTransactionId()),
                "LOAN"
        );

        return loanMapper.toLoanTransactionResponse(loan);
    }

    @Transactional
    public void blockUsersWithReturnOverdue30Days() {
        LocalDate thresholdDate = LocalDate.now().minusDays(30);

        List<LoanEntity> overdueLoans = loanRepository.findLoansOverdueOrWithUnpaidFine(thresholdDate);

        for (LoanEntity loan : overdueLoans) {
            UserEntity user = loan.getUserEntity();
            if (user.getIsActive() != UserStatus.LOCKED) {
                user.setIsActive(UserStatus.LOCKED);
                user.setLockedAt(LocalDateTime.now());
                user.setLockReason("Khóa tài khoản do quá hạn trả sách hoặc nợ phí quá 30 ngày");
                userRepository.save(user);
                sendAccountLockEmail(user);
            }
        }
    }

    private void sendAccountLockEmail(UserEntity user) {
        String toEmail = user.getUsername();
        String subject = "[Thông báo khóa tài khoản thư viện]";
        String body = String.format(
                "Kính gửi %s %s,\n\n" +
                        "Tài khoản thư viện của bạn đã bị khóa do có khoản sách trả quá hạn hoặc khoản phạt chưa thanh toán quá 30 ngày.\n" +
                        "Vui lòng liên hệ quản trị viên để được hỗ trợ mở lại tài khoản.\n\n" +
                        "Trân trọng,\nBan Quản lý Thư viện",
                user.getFirstName() != null ? user.getFirstName() : "",
                user.getLastName() != null ? user.getLastName() : ""
        );

        emailService.sendEmailAsync(toEmail, subject, body);
    }

    @Override
    public Page<LoanResponse> getUserLoansWithStatusOrPaymentStatus(Pageable pageable) {
        log.info("Fetching loans for user with BORROWED status or NON_PAYMENT payment status");

        // Get current user information from SecurityContext
        UserEntity currentUser = authService.getCurrentUser();

        // Query loans with BORROWED status or UNPAID payment status
        Page<LoanEntity> loans = loanRepository.findByUserEntityAndStatusInOrUserEntityAndPaymentStatus(
                currentUser,
                List.of(LoanStatus.BORROWED,LoanStatus.RESERVED),
                currentUser,
                LoanEntity.PaymentStatus.UNPAID,
                pageable
        );

        return loans.map(loanMapper::toLoanTransactionResponse);
    }
}