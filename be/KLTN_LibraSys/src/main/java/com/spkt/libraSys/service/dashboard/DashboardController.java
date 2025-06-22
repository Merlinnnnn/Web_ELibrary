package com.spkt.libraSys.service.dashboard;

import com.spkt.libraSys.exception.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import java.time.LocalDate;
import java.util.Map;
import java.time.format.DateTimeParseException;

/**
 * Controller for dashboard-related endpoints
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Log4j2
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Get all dashboard statistics
     * @return Map containing all dashboard statistics
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStatistics() {
        try {
            Map<String, Object> statistics = dashboardService.getAllStatistics();
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .message("Đã lấy thống kê dashboard thành công")
                    .data(statistics)
                    .build());
        } catch (Exception e) {
            log.error("Lỗi khi lấy thống kê dashboard", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .message("Lỗi khi lấy thống kê dashboard: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get document statistics
     * @return Map containing document statistics
     */
    @GetMapping("/documents")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDocumentStatistics() {
        try {
            Map<String, Object> statistics = dashboardService.getDocumentStatistics();
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .message("Đã lấy thống kê tài liệu thành công")
                    .data(statistics)
                    .build());
        } catch (Exception e) {
            log.error("Lỗi khi lấy thống kê tài liệu", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .message("Lỗi khi lấy thống kê tài liệu: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get loan statistics
     * @return Map containing loan statistics
     */
    @GetMapping("/loans")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLoanStatistics() {
        try {
            Map<String, Object> statistics = dashboardService.getLoanStatistics();
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .message("Đã lấy thống kê mượn sách thành công")
                    .data(statistics)
                    .build());
        } catch (Exception e) {
            log.error("Lỗi khi lấy thống kê mượn sách", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .message("Lỗi khi lấy thống kê mượn sách: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get user statistics
     * @return Map containing user statistics
     */
    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserStatistics() {
        try {
            Map<String, Object> statistics = dashboardService.getUserStatistics();
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .message("Đã lấy thống kê người dùng thành công")
                    .data(statistics)
                    .build());
        } catch (Exception e) {
            log.error("Lỗi khi lấy thống kê người dùng", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .message("Lỗi khi lấy thống kê người dùng: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get DRM statistics
     * @return Map containing DRM statistics
     */
    @GetMapping("/drm")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDrmStatistics() {
        try {
            Map<String, Object> statistics = dashboardService.getDrmStatistics();
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .message("Đã lấy thống kê DRM thành công")
                    .data(statistics)
                    .build());
        } catch (Exception e) {
            log.error("Lỗi khi lấy thống kê DRM", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .message("Lỗi khi lấy thống kê DRM: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get fine statistics
     * @return Map containing fine statistics
     */
    @GetMapping("/fines")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFineStatistics() {
        try {
            Map<String, Object> statistics = dashboardService.getFineStatistics();
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .message("Đã lấy thống kê tiền phạt thành công")
                    .data(statistics)
                    .build());
        } catch (Exception e) {
            log.error("Lỗi khi lấy thống kê tiền phạt", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .message("Lỗi khi lấy thống kê tiền phạt: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get payment statistics
     * @return Map containing payment statistics
     */
    @GetMapping("/payments")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPaymentStatistics() {
        try {
            Map<String, Object> statistics = dashboardService.getPaymentStatistics();
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .message("Đã lấy thống kê thanh toán thành công")
                    .data(statistics)
                    .build());
        } catch (Exception e) {
            log.error("Lỗi khi lấy thống kê thanh toán", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .message("Lỗi khi lấy thống kê thanh toán: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get daily statistics
     * @return Map containing daily statistics
     */
    @GetMapping("/daily")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDailyStatistics() {
        try {
            Map<String, Object> statistics = dashboardService.getDailyStatistics();
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .message("Đã lấy thống kê theo ngày thành công")
                    .data(statistics)
                    .build());
        } catch (Exception e) {
            log.error("Lỗi khi lấy thống kê theo ngày", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .message("Lỗi khi lấy thống kê theo ngày: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get monthly statistics
     * @return Map containing monthly statistics
     */
    @GetMapping("/monthly")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMonthlyStatistics() {
        try {
            Map<String, Object> statistics = dashboardService.getMonthlyStatistics();
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .message("Đã lấy thống kê theo tháng thành công")
                    .data(statistics)
                    .build());
        } catch (Exception e) {
            log.error("Lỗi khi lấy thống kê theo tháng", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .message("Lỗi khi lấy thống kê theo tháng: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get yearly statistics
     * @return Map containing yearly statistics
     */
    @GetMapping("/yearly")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getYearlyStatistics() {
        try {
            Map<String, Object> statistics = dashboardService.getYearlyStatistics();
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .message("Đã lấy thống kê theo năm thành công")
                    .data(statistics)
                    .build());
        } catch (Exception e) {
            log.error("Lỗi khi lấy thống kê theo năm", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .message("Lỗi khi lấy thống kê theo năm: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get document statistics by type
     * @return Map containing document statistics by type
     */
    @GetMapping("/documents/by-type")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDocumentTypeStatistics() {
        try {
            Map<String, Object> statistics = dashboardService.getDocumentTypeStatistics();
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .message("Đã lấy thống kê tài liệu theo loại thành công")
                    .data(statistics)
                    .build());
        } catch (Exception e) {
            log.error("Lỗi khi lấy thống kê tài liệu theo loại", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .message("Lỗi khi lấy thống kê tài liệu theo loại: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get loan statistics for a specific date range
     * @param startDate Start date (format: yyyy-MM-dd)
     * @param endDate End date (format: yyyy-MM-dd)
     * @return Map containing loan statistics for the date range
     */
    @GetMapping("/loans/statistics/date-range")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<Map<String, Object>> getLoanStatisticsByDateRange(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            // Set default values if not provided
            if (startDate == null) {
                startDate = LocalDate.now().minusDays(30);
            }
            if (endDate == null) {
                endDate = LocalDate.now();
            }

            // Validate date range
            if (startDate.isAfter(endDate)) {
                return ApiResponse.<Map<String, Object>>builder()
                    .code(1001)
                    .success(false)
                    .message("Ngày bắt đầu phải trước ngày kết thúc")
                    .build();
            }

            // Validate date values
            if (startDate.getYear() < 1900 || endDate.getYear() > 2100) {
                return ApiResponse.<Map<String, Object>>builder()
                    .code(1001)
                    .success(false)
                    .message("Năm phải nằm trong khoảng từ 1900 đến 2100")
                    .build();
            }

            Map<String, Object> statistics = dashboardService.getLoanStatisticsByDateRange(startDate, endDate);
            return ApiResponse.<Map<String, Object>>builder()
                .code(1000)
                .success(true)
                .message("Lấy thống kê mượn trả sách theo khoảng thời gian thành công")
                .data(statistics)
                .build();

        } catch (DateTimeParseException e) {
            return ApiResponse.<Map<String, Object>>builder()
                .code(1001)
                .success(false)
                .message("Định dạng ngày không hợp lệ. Vui lòng sử dụng định dạng YYYY-MM-DD")
                .build();
        } catch (Exception e) {
            log.error("Error retrieving loan statistics by date range", e);
            return ApiResponse.<Map<String, Object>>builder()
                .code(1002)
                .success(false)
                .message("Lỗi khi lấy thống kê mượn trả sách theo khoảng thời gian")
                .build();
        }
    }
}
