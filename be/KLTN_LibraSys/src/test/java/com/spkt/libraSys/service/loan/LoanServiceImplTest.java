package com.spkt.libraSys.service.loan;

import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;
import com.spkt.libraSys.service.access.AuthService;
import com.spkt.libraSys.service.document.DocumentEntity;
import com.spkt.libraSys.service.document.PhysicalDocument.PhysicalDocumentEntity;
import com.spkt.libraSys.service.document.PhysicalDocument.PhysicalDocumentRepository;
import com.spkt.libraSys.service.email.EmailService;
import com.spkt.libraSys.service.notification.NotificationService;
import com.spkt.libraSys.service.notification.NotificationType;
import com.spkt.libraSys.service.qrcode.JwtTokenData;
import com.spkt.libraSys.service.qrcode.QRService;
import com.spkt.libraSys.service.role.RoleEntity;
import com.spkt.libraSys.service.user.UserEntity;
import com.spkt.libraSys.service.user.UserRepository;
import com.spkt.libraSys.service.user.UserStatus;
import com.spkt.libraSys.service.webSocket.WebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceImplTest {

    @Mock
    private AuthService authService;
    @Mock
    private PhysicalDocumentRepository physicalDocumentRepository;
    @Mock
    private LoanRepository loanRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private LoanMapper loanMapper;
    @Mock
    private QRService qrService;
    @Mock
    private LoanValidationService validationService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private WebSocketService webSocketService;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private LoanServiceImpl loanService;

    private UserEntity testUser;
    private PhysicalDocumentEntity testPhysicalDoc;
    private DocumentEntity testDocument;
    private LoanEntity testLoan;
    private LoanResponse testLoanResponse;
    private RoleEntity adminRole;
    private RoleEntity userRole;

    @BeforeEach
    void setUp() {
        // Setup test roles
        adminRole = RoleEntity.builder()
                .roleName("ADMIN")
                .build();
        userRole = RoleEntity.builder()
                .roleName("USER")
                .build();

        // Setup test user
        testUser = UserEntity.builder()
                .userId("test123")
                .username("testuser@example.com")
                .firstName("Test")
                .lastName("User")
                .isActive(UserStatus.ACTIVE)
                .roleEntities(Set.of(userRole))
                .currentBorrowedCount(0)
                .build();

        // Setup test document
        testDocument = DocumentEntity.builder()
                .documentId(1L)
                .documentName("Test Book")
                .build();

        // Setup test physical document
        testPhysicalDoc = PhysicalDocumentEntity.builder()
                .physicalDocumentId(1L)
                .document(testDocument)
                .price(100000.0)
                .borrowedCount(0)
                .unavailableCount(0)
                .build();

        // Setup test loan
        testLoan = LoanEntity.builder()
                .transactionId(1L)
                .userEntity(testUser)
                .physicalDoc(testPhysicalDoc)
                .status(LoanStatus.RESERVED)
                .loanDate(LocalDateTime.now())
                .build();

        // Setup test loan response
        testLoanResponse = LoanResponse.builder()
                .transactionId(1L)
                .documentId("1")
                .physicalDocId(1L)
                .documentName("Test Book")
                .username("testuser@example.com")
                .status(LoanStatus.RESERVED)
                .build();

        // Set loan duration days
        ReflectionTestUtils.setField(loanService, "loanDurationDays", 14);
    }

    @Test
    void createLoan_Success() {
        // Arrange
        LoanRequest request = new LoanRequest();
        request.setPhysicalDocId(1L);

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(loanRepository.existsByUserEntityAndReturnDateIsNullAndDueDateBefore(any(), any())).thenReturn(false);
        when(loanRepository.existsByUserEntityAndPaymentStatus(any(), any())).thenReturn(false);
        when(physicalDocumentRepository.findById(1L)).thenReturn(Optional.of(testPhysicalDoc));
        when(loanRepository.existsByUserEntityAndPhysicalDocAndStatusIn(any(), any(), any())).thenReturn(false);
        when(userRepository.save(any())).thenReturn(testUser);
        when(loanRepository.save(any())).thenReturn(testLoan);
        when(loanMapper.toLoanTransactionResponse(any())).thenReturn(testLoanResponse);

        // Act
        LoanResponse response = loanService.createLoan(request);

        // Assert
        assertNotNull(response);
        assertEquals(testLoanResponse.getTransactionId(), response.getTransactionId());
        verify(loanRepository).save(any());
        verify(notificationService).createAndSendNotification(any(), any(), any(), any(), any());
    }

    @Test
    void createLoan_UserHasOverdueLoan() {
        // Arrange
        LoanRequest request = new LoanRequest();
        request.setPhysicalDocId(1L);

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(loanRepository.existsByUserEntityAndReturnDateIsNullAndDueDateBefore(any(), any())).thenReturn(true);

        // Act & Assert
        assertThrows(AppException.class, () -> loanService.createLoan(request));
    }

    @Test
    void getUserLoans_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<LoanEntity> loans = Collections.singletonList(testLoan);
        Page<LoanEntity> loanPage = new PageImpl<>(loans, pageable, loans.size());

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(loanRepository.findByUserEntity_UserId(anyString(), any())).thenReturn(loanPage);
        when(loanMapper.toLoanTransactionResponse(any())).thenReturn(testLoanResponse);

        // Act
        Page<LoanResponse> response = loanService.getUserLoans("test123", pageable);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        verify(loanRepository).findByUserEntity_UserId(anyString(), any());
    }

    @Test
    void getLoanId_Success() {
        // Arrange
        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
        when(loanMapper.toLoanTransactionResponse(any())).thenReturn(testLoanResponse);

        // Act
        LoanResponse response = loanService.getLoanId(1L);

        // Assert
        assertNotNull(response);
        assertEquals(testLoanResponse.getTransactionId(), response.getTransactionId());
    }

    @Test
    void getLoanId_NotFound() {
        // Arrange
        when(loanRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AppException.class, () -> loanService.getLoanId(1L));
    }

    @Test
    void handleQrcodeScan_Success() {
        // Arrange
        String action = "reserved";
        String qrToken = "valid.token";
        JwtTokenData tokenData = new JwtTokenData(1L, LoanStatus.RESERVED);

        UserEntity librarian = UserEntity.builder()
                .userId("librarian123")
                .roleEntities(Set.of(adminRole))
                .build();

        when(authService.getCurrentUser()).thenReturn(librarian);
        when(qrService.parseJwtToken(qrToken)).thenReturn(tokenData);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
        when(loanRepository.save(any())).thenReturn(testLoan);
        when(loanMapper.toLoanTransactionResponse(any())).thenReturn(testLoanResponse);

        // Act
        LoanResponse response = loanService.handleQrcodeScan(action, qrToken);

        // Assert
        assertNotNull(response);
        verify(loanRepository).save(any());
        verify(notificationService).createAndSendNotification(any(), any(), any(), any(), any());
    }

    @Test
    void createFineForDamagedOrLostBook_Success() {
        // Arrange
        testLoan.setStatus(LoanStatus.BORROWED);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
        when(loanRepository.save(any())).thenReturn(testLoan);
        when(loanMapper.toLoanTransactionResponse(any())).thenReturn(testLoanResponse);
        testLoan.setReturnCondition(LoanEntity.Condition.NORMAL);
        // Act
        LoanResponse response = loanService.createFineForDamagedOrLostBook(1L);

        // Assert
        assertNotNull(response);
        assertEquals(LoanEntity.PaymentStatus.UNPAID, testLoan.getPaymentStatus());
        assertEquals(LoanEntity.Condition.DAMAGED, testLoan.getReturnCondition());
        verify(loanRepository).save(any());
        verify(notificationService).createAndSendNotification(any(), any(), any(), any(), any());
    }

    @Test
    void processCashPayment_Success() {
        // Arrange
        testLoan.setFineAmount(100000.0);
        testLoan.setPaymentStatus(LoanEntity.PaymentStatus.UNPAID);
        
        UserEntity librarian = UserEntity.builder()
                .username("librarian123")
                .roleEntities(Set.of(adminRole))
                .build();

        when(authService.getCurrentUser()).thenReturn(librarian);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
        when(loanRepository.save(any())).thenReturn(testLoan);
        when(loanMapper.toLoanTransactionResponse(any())).thenReturn(testLoanResponse);

        // Act
        LoanResponse response = loanService.processCashPayment(1L);

        // Assert
        assertNotNull(response);
        assertEquals(LoanEntity.PaymentStatus.CASH, testLoan.getPaymentStatus());
        assertNotNull(testLoan.getPaidAt());
        assertEquals("librarian123", testLoan.getLibrarianName());
        verify(loanRepository).save(any());
        verify(notificationService).createAndSendNotification(any(), any(), any(), any(), any());
    }

    @Test
    void blockUsersWithReturnOverdue30Days_Success() {
        // Arrange
        LocalDate thresholdDate = LocalDate.now().minusDays(30);
        List<LoanEntity> overdueLoans = Collections.singletonList(testLoan);
        when(loanRepository.findLoansOverdueOrWithUnpaidFine(thresholdDate)).thenReturn(overdueLoans);
        when(userRepository.save(any())).thenReturn(testUser);

        // Act
        loanService.blockUsersWithReturnOverdue30Days();

        // Assert
        assertEquals(UserStatus.LOCKED, testUser.getIsActive());
        assertNotNull(testUser.getLockedAt());
        verify(userRepository).save(any());
        verify(emailService).sendEmailAsync(anyString(), anyString(), anyString());
    }

    @Test
    void getUserLoansWithStatusOrPaymentStatus_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<LoanEntity> loans = Collections.singletonList(testLoan);
        Page<LoanEntity> loanPage = new PageImpl<>(loans, pageable, loans.size());

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(loanRepository.findByUserEntityAndStatusInOrUserEntityAndPaymentStatus(
                any(), any(), any(), any(), any())).thenReturn(loanPage);
        when(loanMapper.toLoanTransactionResponse(any())).thenReturn(testLoanResponse);

        // Act
        Page<LoanResponse> response = loanService.getUserLoansWithStatusOrPaymentStatus(pageable);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        verify(loanRepository).findByUserEntityAndStatusInOrUserEntityAndPaymentStatus(
                any(), any(), any(), any(), any());
    }
} 