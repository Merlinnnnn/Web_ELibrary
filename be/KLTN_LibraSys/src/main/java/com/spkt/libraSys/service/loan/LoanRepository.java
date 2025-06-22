package com.spkt.libraSys.service.loan;

import com.spkt.libraSys.service.document.DocumentEntity;
import com.spkt.libraSys.service.document.PhysicalDocument.PhysicalDocumentEntity;
import com.spkt.libraSys.service.user.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
public interface LoanRepository extends JpaRepository<LoanEntity, Long>, JpaSpecificationExecutor<LoanEntity> {
    @Query("SELECT COUNT(lt) > 0 FROM loans lt " +
            "WHERE lt.userEntity = :user " +
            "AND lt.physicalDoc = :physicalDocument " +
            "AND lt.status = :status")
    boolean existsPendingLoanTransaction(@Param("user") UserEntity user,
                                         @Param("physicalDocument") PhysicalDocumentEntity physicalDocument,
                                         @Param("status") LoanStatus status);

    @Query("SELECT COUNT(l) > 0 FROM loans l " +
            "WHERE l.userEntity = :user AND " +
            "l.physicalDoc.physicalDocumentId = :physicalId " +
            "AND l.status = :status")
    boolean existsByUserEntityAndDocEntityAndStatus(@Param("user") UserEntity user, @Param("physicalId") Long physicalId, @Param("status") LoanStatus status);

    Page<LoanEntity> findByUserEntity(UserEntity user, Pageable page);

    Page<LoanEntity> findByUserEntity_UserId(String userEntityUserId,
                                             Pageable pageable);
    @Query("SELECT COUNT(l) > 0 FROM loans l " +
           "WHERE l.userEntity = :user AND " +
           "l.physicalDoc.physicalDocumentId = :physicalId " +
           "AND l.status IN :statuses")
    boolean existsByUserEntityAndPhysicalDocAndStatusIn(@Param("user") UserEntity user, 
                                                       @Param("physicalId") Long physicalId, 
                                                       @Param("statuses") Collection<LoanStatus> statuses);

    @Query("SELECT COUNT(l) FROM loans l WHERE l.status = :status")
    long countByStatus(@Param("status") LoanStatus status);

    @Query("SELECT COUNT(l) FROM loans l WHERE l.dueDate < :today AND l.status = :status")
    long countOverdueLoans(@Param("today") LocalDate today, @Param("status") LoanStatus status);

    @Query("SELECT COUNT(l) FROM loans l WHERE l.createdAt > :date")
    long countRecentLoans(@Param("date") LocalDateTime date);

    List<LoanEntity> findByStatus(LoanStatus status);

    List<LoanEntity> findByStatusAndDueDateBetween(LoanStatus status, LocalDate today,LocalDate threeDaysLater);

    Optional<LoanEntity> findByVnpTxnRef(String vnpTxnRef);

    @Query("SELECT COUNT(l) FROM loans l WHERE l.paymentStatus IN :statuses")
    long countByPaymentStatusIn(@Param("statuses") Set<LoanEntity.PaymentStatus> statuses);

    @Query("SELECT COALESCE(SUM(l.fineAmount), 0) FROM loans l WHERE l.paymentStatus IN :statuses")
    double sumFineAmountByPaymentStatusIn(@Param("statuses") Set<LoanEntity.PaymentStatus> statuses);

    @Query("SELECT COUNT(l) FROM loans l WHERE l.paymentStatus IN :statuses AND l.paidAt BETWEEN :startDate AND :endDate")
    long countByPaymentStatusInAndPaidAtBetween(
        @Param("statuses") Set<LoanEntity.PaymentStatus> statuses,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT COALESCE(SUM(l.fineAmount), 0) FROM loans l WHERE l.paymentStatus IN :statuses AND l.paidAt BETWEEN :startDate AND :endDate")
    double sumFineAmountByPaymentStatusInAndPaidAtBetween(
        @Param("statuses") Set<LoanEntity.PaymentStatus> statuses,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT COUNT(l) FROM loans l WHERE l.paymentStatus = :status")
    long countByPaymentStatus(@Param("status") LoanEntity.PaymentStatus status);

    @Query("SELECT COUNT(l) FROM loans l WHERE l.paymentStatus = :status AND l.vnpTxnRef IS NOT NULL")
    long countByPaymentStatusAndVnpTxnRefIsNotNull(@Param("status") LoanEntity.PaymentStatus status);

    @Query("SELECT COALESCE(SUM(l.fineAmount), 0) FROM loans l WHERE l.paymentStatus = :status")
    double sumFineAmountByPaymentStatus(@Param("status") LoanEntity.PaymentStatus status);

    @Query("SELECT COALESCE(SUM(l.fineAmount), 0) FROM loans l")
    double sumFineAmount();

    @Query("SELECT COUNT(l) FROM loans l WHERE l.fineAmount > :amount")
    long countByFineAmountGreaterThan(@Param("amount") double amount);

    @Query("SELECT COUNT(l) FROM loans l WHERE l.status = :status AND l.createdAt BETWEEN :startDate AND :endDate")
    long countByStatusAndCreatedAtBetween(
        @Param("status") LoanStatus status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT COUNT(l) FROM loans l WHERE l.status = :status AND l.returnDate BETWEEN :startDate AND :endDate")
    long countByStatusAndReturnDateBetween(
        @Param("status") LoanStatus status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT COUNT(l) FROM loans l WHERE l.fineAmount > :amount AND l.createdAt BETWEEN :startDate AND :endDate")
    long countByFineAmountGreaterThanAndCreatedAtBetween(
        @Param("amount") double amount,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT COALESCE(SUM(l.fineAmount), 0) FROM loans l WHERE l.createdAt BETWEEN :startDate AND :endDate")
    double sumFineAmountByCreatedAtBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT l FROM loans l WHERE l.createdAt >= :date")
    List<LoanEntity> findRecentLoans(@Param("date") LocalDateTime date);

    List<LoanEntity> findByUserEntityAndPaymentStatus(UserEntity user, LoanEntity.PaymentStatus paymentStatus);

    List<LoanEntity> findByUserEntity(UserEntity userEntity);

    @Query("SELECT DISTINCT l FROM loans l LEFT JOIN FETCH l.physicalDoc pd LEFT JOIN FETCH pd.document WHERE l.userEntity = :user")
    List<LoanEntity> findByUserEntityWithDocuments(@Param("user") UserEntity user);

    boolean existsByUserEntityAndReturnDateIsNullAndDueDateBefore(UserEntity user, LocalDate date);

    @Query("SELECT l FROM loans l WHERE " +
            "(l.returnDate IS NULL AND l.dueDate <= :thresholdDate) " +
            "OR (" +
            "  l.paymentStatus = com.spkt.libraSys.service.loan.LoanEntity.PaymentStatus.UNPAID " +
            "   AND l.returnDate IS NOT NULL " +
            "   AND l.returnDate <= :thresholdDate" +
            ")")
    List<LoanEntity> findLoansOverdueOrWithUnpaidFine(@Param("thresholdDate") LocalDate thresholdDate);

    boolean existsByUserEntityAndPaymentStatus(UserEntity user, LoanEntity.PaymentStatus paymentStatus);


    Page<LoanEntity> findByUserEntityAndStatusInOrUserEntityAndPaymentStatus(
            UserEntity userEntity, List<LoanStatus> status, UserEntity userEntity2, LoanEntity.PaymentStatus paymentStatus, Pageable pageable
    );

    long countByUserEntity_UserIdAndStatusNotIn(String userId, List<LoanStatus> statuses);

    @Query("SELECT COUNT(l) FROM loans l WHERE l.createdAt BETWEEN :startDate AND :endDate")
    long countByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(l) FROM loans l WHERE l.returnDate BETWEEN :startDate AND :endDate")
    long countByReturnDateBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(l) FROM loans l WHERE l.status = :status AND l.createdAt < :date")
    long countByStatusAndCreatedAtBefore(@Param("status") LoanStatus status, @Param("date") LocalDateTime date);

    @Query("SELECT COUNT(l) FROM loans l WHERE l.status = :status AND l.dueDate < :date")
    long countByStatusAndDueDateBefore(@Param("status") LoanStatus status, @Param("date") LocalDate date);

    @Query("SELECT new map(l.status as status, COUNT(l) as count) FROM loans l WHERE l.createdAt BETWEEN :startDate AND :endDate GROUP BY l.status")
    List<Map<String, Object>> countByStatusGroupedByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT DATE(l.createdAt) as date, COUNT(l) as count FROM loans l WHERE l.createdAt BETWEEN :startDate AND :endDate GROUP BY DATE(l.createdAt)")
    Map<LocalDate, Long> countDailyLoansBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT DATE(l.returnDate) as date, COUNT(l) as count FROM loans l WHERE l.returnDate BETWEEN :startDate AND :endDate GROUP BY DATE(l.returnDate)")
    Map<LocalDate, Long> countDailyReturnsBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
