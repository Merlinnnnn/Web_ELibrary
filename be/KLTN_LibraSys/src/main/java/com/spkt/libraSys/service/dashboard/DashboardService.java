package com.spkt.libraSys.service.dashboard;

import java.util.Map;
import java.time.LocalDate;

/**
 * Service interface for dashboard-related operations
 */
public interface DashboardService {
    
    /**
     * Get document statistics for dashboard
     * @return Map containing document statistics including:
     * - totalDocuments: Total number of documents
     * - documentsByStatus: Number of documents by each status
     * - documentsByType: Number of documents by each type
     * - documentsByCategory: Number of documents by each category
     */
    Map<String, Object> getDocumentStatistics();
    
    /**
     * Get loan statistics for dashboard
     * @return Map containing loan statistics including:
     * - totalLoans: Total number of loans
     * - activeLoans: Number of active loans
     * - overdueLoans: Number of overdue loans
     * - recentLoans: Number of loans in last 30 days
     * - loansByStatus: Number of loans by each status
     */
    Map<String, Object> getLoanStatistics();
    
    /**
     * Get user statistics for dashboard
     * @return Map containing user statistics including:
     * - totalUsers: Total number of users
     * - activeUsers: Number of active users
     * - newUsers: Number of new users in last 30 days
     * - usersByRole: Number of users by each role
     * - usersByStatus: Number of users by each status
     */
    Map<String, Object> getUserStatistics();
    
    /**
     * Get DRM license statistics for dashboard
     * @return Map containing DRM license statistics including:
     * - totalLicenses: Total number of licenses
     * - activeLicenses: Number of active licenses
     * - revokedLicenses: Number of revoked licenses
     * - recentLicenses: Number of licenses in last 30 days
     * - licensesByStatus: Number of licenses by each status
     */
    Map<String, Object> getDrmStatistics();

    /**
     * Get fine statistics for dashboard
     * @return Map containing fine statistics including:
     * - totalFines: Total amount of fines
     * - paidFines: Amount of paid fines
     * - pendingFines: Amount of pending fines
     * - totalFineTransactions: Total number of fine transactions
     * - paidTransactions: Number of paid transactions
     * - pendingTransactions: Number of pending transactions
     * - finesByMonth: Fine amounts by month
     */
    Map<String, Object> getFineStatistics();

    /**
     * Get payment statistics for dashboard
     * @return Map containing payment statistics including:
     * - totalPayments: Total number of payments
     * - cashPayments: Number of cash payments
     * - vnpayPayments: Number of VNPAY payments
     * - totalAmount: Total payment amount
     * - cashAmount: Cash payment amount
     * - vnpayAmount: VNPAY payment amount
     * - paymentsByMonth: Payment amounts by month
     * - paymentMethodDistribution: Distribution of payment methods
     */
    Map<String, Object> getPaymentStatistics();

    /**
     * Get daily statistics for dashboard
     * @return Map containing daily statistics including:
     * - newLoans: Number of new loans today
     * - returns: Number of returns today
     * - newFines: Number of new fines today
     * - newFineAmount: Amount of new fines today
     * - payments: Number of payments today
     * - paymentAmount: Amount of payments today
     * - activeUsers: Number of active users today
     */
    Map<String, Object> getDailyStatistics();

    /**
     * Get monthly statistics for dashboard
     * @return Map containing monthly statistics including:
     * - loansByMonth: Number of loans by month
     * - returnsByMonth: Number of returns by month
     * - finesByMonth: Fine amounts by month
     * - paymentsByMonth: Payment amounts by month
     * - usersByMonth: Number of new users by month
     */
    Map<String, Object> getMonthlyStatistics();

    /**
     * Get yearly statistics for dashboard
     * @return Map containing yearly statistics including:
     * - loansByYear: Number of loans by year
     * - returnsByYear: Number of returns by year
     * - finesByYear: Fine amounts by year
     * - paymentsByYear: Payment amounts by year
     * - usersByYear: Number of new users by year
     */
    Map<String, Object> getYearlyStatistics();
    
    /**
     * Get all dashboard statistics in a single call
     * @return Map containing all dashboard statistics including:
     * - documents: Document statistics
     * - loans: Loan statistics
     * - users: User statistics
     * - drm: DRM statistics
     * - fines: Fine statistics
     * - payments: Payment statistics
     * - daily: Daily statistics
     * - monthly: Monthly statistics
     * - yearly: Yearly statistics
     */
    Map<String, Object> getAllStatistics();

    /**
     * Get document statistics by type
     * @return Map containing document statistics by type including:
     * - totalByType: Total number of documents by each type
     * - typeDistribution: Distribution of documents by type
     */
    Map<String, Object> getDocumentTypeStatistics();

    /**
     * Get loan statistics for a specific date range
     * @param startDate Start date of the range
     * @param endDate End date of the range
     * @return Map containing loan statistics including:
     * - totalLoans: Total number of loans in the period
     * - totalReturns: Total number of returns in the period
     * - activeLoans: Number of active loans
     * - overdueLoans: Number of overdue loans
     * - loansByStatus: Number of loans by each status
     * - dailyLoans: Number of loans per day
     * - dailyReturns: Number of returns per day
     */
    Map<String, Object> getLoanStatisticsByDateRange(LocalDate startDate, LocalDate endDate);
}