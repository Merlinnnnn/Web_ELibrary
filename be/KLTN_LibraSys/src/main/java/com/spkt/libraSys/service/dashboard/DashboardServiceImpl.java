package com.spkt.libraSys.service.dashboard;

import com.spkt.libraSys.service.document.DocumentEntity;
import com.spkt.libraSys.service.document.DocumentRepository;
import com.spkt.libraSys.service.document.DocumentStatus;
import com.spkt.libraSys.service.document.DocumentType.DocumentTypeEntity;
import com.spkt.libraSys.service.drm.DrmLicenseEntity;
import com.spkt.libraSys.service.drm.DrmLicenseRepository;
import com.spkt.libraSys.service.loan.LoanEntity;
import com.spkt.libraSys.service.loan.LoanRepository;
import com.spkt.libraSys.service.loan.LoanStatus;
import com.spkt.libraSys.service.user.UserEntity;
import com.spkt.libraSys.service.user.UserRepository;
import com.spkt.libraSys.service.user.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of the DashboardService interface
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class DashboardServiceImpl implements DashboardService {

    private final DocumentRepository documentRepository;
    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final DrmLicenseRepository drmLicenseRepository;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDocumentStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        try {
            // Total documents
            long totalDocuments = documentRepository.count();
            statistics.put("totalDocuments", totalDocuments);

            // Documents by status
            for (DocumentStatus status : DocumentStatus.values()) {
                try {
                    long countByStatus = documentRepository.countByStatus(status);
                    statistics.put("documentsBy" + status.name(), countByStatus);
                } catch (Exception e) {
                    log.error("Error counting documents by status: " + status, e);
                    statistics.put("documentsBy" + status.name(), 0L);
                }
            }

        } catch (Exception e) {
            log.error("Error retrieving document statistics", e);
            statistics.put("error", "Không thể lấy thống kê tài liệu");
        }

        return statistics;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getLoanStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        try {
            // Total loans
            long totalLoans = loanRepository.count();
            statistics.put("totalLoans", totalLoans);

            // Active loans (BORROWED status)
            try {
                long activeLoans = loanRepository.countByStatus(LoanStatus.BORROWED);
                statistics.put("activeLoans", activeLoans);
            } catch (Exception e) {
                log.error("Error counting active loans", e);
                statistics.put("activeLoans", 0L);
            }

            // Overdue loans
            try {
                LocalDate today = LocalDate.now();
                long overdueLoans = loanRepository.countOverdueLoans(today, LoanStatus.BORROWED);
                statistics.put("overdueLoans", overdueLoans);
            } catch (Exception e) {
                log.error("Error counting overdue loans", e);
                statistics.put("overdueLoans", 0L);
            }

            // Loans created in the last 30 days
            try {
                LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
                long recentLoans = loanRepository.countRecentLoans(thirtyDaysAgo);
                statistics.put("recentLoans", recentLoans);
            } catch (Exception e) {
                log.error("Error counting recent loans", e);
                statistics.put("recentLoans", 0L);
            }

        } catch (Exception e) {
            log.error("Error retrieving loan statistics", e);
            statistics.put("error", "Không thể lấy thống kê mượn trả");
        }

        return statistics;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getUserStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        try {
            // Total users
            long totalUsers = userRepository.count();
            statistics.put("totalUsers", totalUsers);

            // Active users
            try {
                long activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);
                statistics.put("activeUsers", activeUsers);
            } catch (Exception e) {
                log.error("Error counting active users", e);
                statistics.put("activeUsers", 0L);
            }

            // New users in the last 30 days
            try {
                LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
                long newUsers = userRepository.countNewUsers(thirtyDaysAgo);
                statistics.put("newUsers", newUsers);
            } catch (Exception e) {
                log.error("Error counting new users", e);
                statistics.put("newUsers", 0L);
            }

        } catch (Exception e) {
            log.error("Error retrieving user statistics", e);
            statistics.put("error", "Không thể lấy thống kê người dùng");
        }

        return statistics;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDrmStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        try {
            // Total DRM licenses
            long totalLicenses = drmLicenseRepository.count();
            statistics.put("totalLicenses", totalLicenses);

            // Active licenses (not revoked)
            try {
                long activeLicenses = drmLicenseRepository.countActivelicenses();
                statistics.put("activeLicenses", activeLicenses);
            } catch (Exception e) {
                log.error("Error counting active licenses", e);
                statistics.put("activeLicenses", 0L);
            }

            // Revoked licenses
            try {
                long revokedLicenses = drmLicenseRepository.countRevokedLicenses();
                statistics.put("revokedLicenses", revokedLicenses);
            } catch (Exception e) {
                log.error("Error counting revoked licenses", e);
                statistics.put("revokedLicenses", 0L);
            }

            // Licenses issued in the last 30 days
            try {
                LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
                long recentLicenses = drmLicenseRepository.countRecentLicenses(thirtyDaysAgo);
                statistics.put("recentLicenses", recentLicenses);
            } catch (Exception e) {
                log.error("Error counting recent licenses", e);
                statistics.put("recentLicenses", 0L);
            }

        } catch (Exception e) {
            log.error("Error retrieving DRM statistics", e);
            statistics.put("error", "Không thể lấy thống kê DRM");
        }

        return statistics;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getFineStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        try {
            // Total fine amount
            double totalFines = loanRepository.sumFineAmount();
            statistics.put("totalFines", totalFines);

            // Paid fine amount
            double paidFines = loanRepository.sumFineAmountByPaymentStatusIn(
                Set.of(LoanEntity.PaymentStatus.CASH, LoanEntity.PaymentStatus.VNPAY)
            );
            statistics.put("paidFines", paidFines);

            // Pending fine amount
            double pendingFines = loanRepository.sumFineAmountByPaymentStatus(LoanEntity.PaymentStatus.UNPAID);
            statistics.put("pendingFines", pendingFines);

            // Total fine transactions
            long totalFineTransactions = loanRepository.countByFineAmountGreaterThan(0);
            statistics.put("totalFineTransactions", totalFineTransactions);

            // Paid transactions
            long paidTransactions = loanRepository.countByPaymentStatusIn(
                Set.of(LoanEntity.PaymentStatus.CASH, LoanEntity.PaymentStatus.VNPAY)
            );
            statistics.put("paidTransactions", paidTransactions);

            // Pending transactions
            long pendingTransactions = loanRepository.countByPaymentStatus(LoanEntity.PaymentStatus.UNPAID);
            statistics.put("pendingTransactions", pendingTransactions);

        } catch (Exception e) {
            log.error("Error retrieving fine statistics", e);
            statistics.put("error", "Không thể lấy thống kê tiền phạt");
        }

        return statistics;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getPaymentStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        try {
            // Total payment transactions
            long totalPayments = loanRepository.countByPaymentStatusIn(
                Set.of(LoanEntity.PaymentStatus.CASH, LoanEntity.PaymentStatus.VNPAY)
            );
            statistics.put("totalPayments", totalPayments);

            // Cash transactions
            long cashPayments = loanRepository.countByPaymentStatus(LoanEntity.PaymentStatus.CASH);
            statistics.put("cashPayments", cashPayments);

            // VNPAY transactions
            long vnpayPayments = loanRepository.countByPaymentStatus(LoanEntity.PaymentStatus.VNPAY);
            statistics.put("vnpayPayments", vnpayPayments);

            // Total payment amount
            double totalAmount = loanRepository.sumFineAmountByPaymentStatusIn(
                Set.of(LoanEntity.PaymentStatus.CASH, LoanEntity.PaymentStatus.VNPAY)
            );
            statistics.put("totalAmount", totalAmount);

            // Cash payment amount
            double cashAmount = loanRepository.sumFineAmountByPaymentStatus(LoanEntity.PaymentStatus.CASH);
            statistics.put("cashAmount", cashAmount);

            // VNPAY payment amount
            double vnpayAmount = loanRepository.sumFineAmountByPaymentStatus(LoanEntity.PaymentStatus.VNPAY);
            statistics.put("vnpayAmount", vnpayAmount);

        } catch (Exception e) {
            log.error("Error retrieving payment statistics", e);
            statistics.put("error", "Không thể lấy thống kê thanh toán");
        }

        return statistics;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDailyStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime now = LocalDateTime.now();

        try {
            // New loan transactions
            long newLoans = loanRepository.countByStatusAndCreatedAtBetween(
                LoanStatus.BORROWED, today, now
            );
            statistics.put("newLoans", newLoans);

            // Return transactions
            long returns = loanRepository.countByStatusAndReturnDateBetween(
                LoanStatus.RETURNED, today, now
            );
            statistics.put("returns", returns);

            // New fine transactions
            long newFines = loanRepository.countByFineAmountGreaterThanAndCreatedAtBetween(
                0, today, now
            );
            statistics.put("newFines", newFines);

            // New fine amount
            double newFineAmount = loanRepository.sumFineAmountByCreatedAtBetween(today, now);
            statistics.put("newFineAmount", newFineAmount);

            // Payment transactions
            long payments = loanRepository.countByPaymentStatusInAndPaidAtBetween(
                Set.of(LoanEntity.PaymentStatus.CASH, LoanEntity.PaymentStatus.VNPAY),
                today, now
            );
            statistics.put("payments", payments);

            // Payment amount
            double paymentAmount = loanRepository.sumFineAmountByPaymentStatusInAndPaidAtBetween(
                Set.of(LoanEntity.PaymentStatus.CASH, LoanEntity.PaymentStatus.VNPAY),
                today, now
            );
            statistics.put("paymentAmount", paymentAmount);

        } catch (Exception e) {
            log.error("Error retrieving daily statistics", e);
            statistics.put("error", "Không thể lấy thống kê theo ngày");
        }

        return statistics;
    }

    @Override
    public Map<String, Object> getAllStatistics() {
        Map<String, Object> allStatistics = new HashMap<>();

        allStatistics.put("documents", getDocumentStatistics());
        allStatistics.put("loans", getLoanStatistics());
        allStatistics.put("users", getUserStatistics());
        allStatistics.put("drm", getDrmStatistics());
        allStatistics.put("fines", getFineStatistics());
        allStatistics.put("payments", getPaymentStatistics());
        allStatistics.put("daily", getDailyStatistics());

        return allStatistics;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getMonthlyStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        LocalDateTime startOfYear = LocalDateTime.now().withMonth(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime now = LocalDateTime.now();

        try {
            // Loan and return statistics by month
            Map<Integer, Long> loansByMonth = new HashMap<>();
            Map<Integer, Long> returnsByMonth = new HashMap<>();
            for (int month = 1; month <= 12; month++) {
                LocalDateTime startOfMonth = startOfYear.withMonth(month);
                LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusSeconds(1);

                long loans = loanRepository.countByStatusAndCreatedAtBetween(
                    LoanStatus.BORROWED, startOfMonth, endOfMonth
                );
                loansByMonth.put(month, loans);

                long returns = loanRepository.countByStatusAndReturnDateBetween(
                    LoanStatus.RETURNED, startOfMonth, endOfMonth
                );
                returnsByMonth.put(month, returns);
            }
            statistics.put("loansByMonth", loansByMonth);
            statistics.put("returnsByMonth", returnsByMonth);

            // Fine statistics by month
            Map<Integer, Double> finesByMonth = new HashMap<>();
            for (int month = 1; month <= 12; month++) {
                LocalDateTime startOfMonth = startOfYear.withMonth(month);
                LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusSeconds(1);

                double fines = loanRepository.sumFineAmountByCreatedAtBetween(startOfMonth, endOfMonth);
                finesByMonth.put(month, fines);
            }
            statistics.put("finesByMonth", finesByMonth);

            // Payment statistics by month
            Map<Integer, Double> paymentsByMonth = new HashMap<>();
            for (int month = 1; month <= 12; month++) {
                LocalDateTime startOfMonth = startOfYear.withMonth(month);
                LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusSeconds(1);

                double payments = loanRepository.sumFineAmountByPaymentStatusInAndPaidAtBetween(
                    Set.of(LoanEntity.PaymentStatus.CASH, LoanEntity.PaymentStatus.VNPAY),
                    startOfMonth, endOfMonth
                );
                paymentsByMonth.put(month, payments);
            }
            statistics.put("paymentsByMonth", paymentsByMonth);

            // New user statistics by month
            Map<Integer, Long> usersByMonth = new HashMap<>();
            for (int month = 1; month <= 12; month++) {
                LocalDateTime startOfMonth = startOfYear.withMonth(month);
                LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusSeconds(1);

                long newUsers = userRepository.countNewUsers(startOfMonth.toLocalDate());
                usersByMonth.put(month, newUsers);
            }
            statistics.put("usersByMonth", usersByMonth);

        } catch (Exception e) {
            log.error("Error retrieving monthly statistics", e);
            statistics.put("error", "Không thể lấy thống kê theo tháng");
        }

        return statistics;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getYearlyStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        int currentYear = LocalDateTime.now().getYear();
        int startYear = currentYear - 4; // Statistics for the last 5 years

        try {
            // Loan and return statistics by year
            Map<Integer, Long> loansByYear = new HashMap<>();
            Map<Integer, Long> returnsByYear = new HashMap<>();
            for (int year = startYear; year <= currentYear; year++) {
                LocalDateTime startOfYear = LocalDateTime.of(year, 1, 1, 0, 0, 0);
                LocalDateTime endOfYear = LocalDateTime.of(year, 12, 31, 23, 59, 59);

                long loans = loanRepository.countByStatusAndCreatedAtBetween(
                    LoanStatus.BORROWED, startOfYear, endOfYear
                );
                loansByYear.put(year, loans);

                long returns = loanRepository.countByStatusAndReturnDateBetween(
                    LoanStatus.RETURNED, startOfYear, endOfYear
                );
                returnsByYear.put(year, returns);
            }
            statistics.put("loansByYear", loansByYear);
            statistics.put("returnsByYear", returnsByYear);

            // Fine statistics by year
            Map<Integer, Double> finesByYear = new HashMap<>();
            for (int year = startYear; year <= currentYear; year++) {
                LocalDateTime startOfYear = LocalDateTime.of(year, 1, 1, 0, 0, 0);
                LocalDateTime endOfYear = LocalDateTime.of(year, 12, 31, 23, 59, 59);

                double fines = loanRepository.sumFineAmountByCreatedAtBetween(startOfYear, endOfYear);
                finesByYear.put(year, fines);
            }
            statistics.put("finesByYear", finesByYear);

            // Payment statistics by year
            Map<Integer, Double> paymentsByYear = new HashMap<>();
            for (int year = startYear; year <= currentYear; year++) {
                LocalDateTime startOfYear = LocalDateTime.of(year, 1, 1, 0, 0, 0);
                LocalDateTime endOfYear = LocalDateTime.of(year, 12, 31, 23, 59, 59);

                double payments = loanRepository.sumFineAmountByPaymentStatusInAndPaidAtBetween(
                    Set.of(LoanEntity.PaymentStatus.CASH, LoanEntity.PaymentStatus.VNPAY),
                    startOfYear, endOfYear
                );
                paymentsByYear.put(year, payments);
            }
            statistics.put("paymentsByYear", paymentsByYear);

            // New user statistics by year
            Map<Integer, Long> usersByYear = new HashMap<>();
            for (int year = startYear; year <= currentYear; year++) {
                LocalDateTime startOfYear = LocalDateTime.of(year, 1, 1, 0, 0, 0);
                LocalDateTime endOfYear = LocalDateTime.of(year, 12, 31, 23, 59, 59);

                long newUsers = userRepository.countNewUsers(startOfYear.toLocalDate());
                usersByYear.put(year, newUsers);
            }
            statistics.put("usersByYear", usersByYear);

        } catch (Exception e) {
            log.error("Error retrieving yearly statistics", e);
            statistics.put("error", "Không thể lấy thống kê theo năm");
        }

        return statistics;
    }

    @Override
    public Map<String, Object> getDocumentTypeStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        // Get all documents with document types
        List<DocumentEntity> documents = documentRepository.findAllWithDocumentTypes();
        
        // Count documents by type
        Map<String, Long> totalByType = documents.stream()
            .flatMap(doc -> doc.getDocumentTypes().stream())
            .collect(Collectors.groupingBy(
                DocumentTypeEntity::getTypeName,
                Collectors.counting()
            ));
        statistics.put("totalByType", totalByType);

        // Calculate distribution ratio
        long totalDocuments = documents.size();
        Map<String, Double> typeDistribution = new HashMap<>();
        totalByType.forEach((type, count) ->
            typeDistribution.put(type, (double) count / totalDocuments * 100)
        );
        statistics.put("typeDistribution", typeDistribution);
        
        return statistics;
    }

    @Override
    public Map<String, Object> getLoanStatisticsByDateRange(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> statistics = new HashMap<>();
        
        try {
            // Get total loans and returns in date range
            long totalLoans = loanRepository.countByStatusAndCreatedAtBetween(
                LoanStatus.BORROWED, startDate.atStartOfDay(), endDate.atTime(23, 59, 59));
            long totalReturns = loanRepository.countByStatusAndCreatedAtBetween(
                LoanStatus.RETURNED, startDate.atStartOfDay(), endDate.atTime(23, 59, 59));

            statistics.put("totalLoans", totalLoans);
            statistics.put("totalReturns", totalReturns);

            // Get active and overdue loans
            long activeLoans = loanRepository.countByStatusAndDueDateBefore(
                LoanStatus.BORROWED, LocalDate.now());
            long overdueLoans = loanRepository.countByStatusAndDueDateBefore(
                LoanStatus.BORROWED, LocalDate.now());

            statistics.put("activeLoans", activeLoans);
            statistics.put("overdueLoans", overdueLoans);

            // Get loan statistics by status
            List<Map<String, Object>> loansByStatusList = loanRepository.countByStatusGroupedByDateRange(
                startDate.atStartOfDay(),
                endDate.atTime(23, 59, 59)
            );
            Map<LoanStatus, Long> loansByStatus = new HashMap<>();
            for (Map<String, Object> statusCount : loansByStatusList) {
                LoanStatus status = (LoanStatus) statusCount.get("status");
                Long count = ((Number) statusCount.get("count")).longValue();
                loansByStatus.put(status, count);
            }
            statistics.put("loansByStatus", loansByStatus);

            // Get daily loan and return counts
            Map<LocalDate, Long> dailyLoans = loanRepository.countDailyLoansBetween(
                startDate.atStartOfDay(),
                endDate.atTime(23, 59, 59)
            );
            Map<LocalDate, Long> dailyReturns = loanRepository.countDailyReturnsBetween(
                startDate.atStartOfDay(),
                endDate.atTime(23, 59, 59)
            );

            statistics.put("dailyLoans", dailyLoans);
            statistics.put("dailyReturns", dailyReturns);

            // Calculate return and overdue rates
            if (totalLoans > 0) {
                double returnRate = (double) totalReturns / totalLoans * 100;
                double overdueRate = (double) overdueLoans / activeLoans * 100;
                
                statistics.put("returnRate", returnRate);
                statistics.put("overdueRate", overdueRate);
            }

        } catch (Exception e) {
            log.error("Error retrieving loan statistics by date range", e);
            statistics.put("error", "Không thể lấy thống kê mượn trả theo khoảng thời gian");
        }

        return statistics;
    }
}
