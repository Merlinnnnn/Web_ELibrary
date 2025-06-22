package com.spkt.libraSys.exception;

import lombok.Getter;
import org.apache.http.protocol.HTTP;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    // User-related errors
    PASSWORD_FAILED(1001, "Không tìm thấy người dùng", HttpStatus.NOT_FOUND),
    USER_NOT_FOUND(1001, "Không tìm thấy người dùng", HttpStatus.NOT_FOUND),
    DUPLICATE_USER(1002, "Người dùng đã tồn tại", HttpStatus.BAD_REQUEST),
    INVALID_CREDENTIALS(1003, "Thông tin đăng nhập không hợp lệ", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED(1004, "Truy cập bị từ chối", HttpStatus.FORBIDDEN),
    UNAUTHORIZED(1005, "Không có quyền truy cập", HttpStatus.UNAUTHORIZED),
    INSUFFICIENT_PERMISSIONS(1006, "Người dùng không có đủ quyền", HttpStatus.FORBIDDEN),
    USER_ACCOUNT_LOCKED(1007, "Tài khoản người dùng đã bị khóa", HttpStatus.FORBIDDEN),
    USER_ACCOUNT_DISABLED(1008, "Tài khoản người dùng đã bị vô hiệu hóa", HttpStatus.FORBIDDEN),
    PASSWORD_EXPIRED(1009, "Mật khẩu đã hết hạn", HttpStatus.UNAUTHORIZED),
    USER_DEACTIVATED(1010, "Tài khoản người dùng đã bị vô hiệu hóa", HttpStatus.FORBIDDEN),
    USER_LOCKED(1011, "Tài khoản người dùng đã bị khóa", HttpStatus.FORBIDDEN),
    USER_ALREADY_DELETED(1012, "Tài khoản người dùng đã bị xóa", HttpStatus.GONE),
    USER_PENDING(1013, "Tài khoản người dùng đang chờ xác minh", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED_ACCESS(1014,"Không có quyền truy cập", HttpStatus.UNAUTHORIZED),
    USER_ALREADY_EXISTS(1015,"Người dùng đã tồn tại",HttpStatus.BAD_REQUEST),
    USER_HAS_OVERDUE_LOAN(1016, "Người dùng có khoản vay quá hạn chưa trả", HttpStatus.BAD_REQUEST),
    // Authentication errors
    TOKEN_EXPIRED(2001, "Token đã hết hạn", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID(2002, "Token không hợp lệ", HttpStatus.UNAUTHORIZED),
    TOKEN_MISSING(2003, "Thiếu token", HttpStatus.UNAUTHORIZED),
    RATE_LIMIT_EXCEEDED(2004, "Đã vượt quá giới hạn yêu cầu", HttpStatus.TOO_MANY_REQUESTS),
    SESSION_EXPIRED(2005, "Phiên làm việc đã hết hạn", HttpStatus.UNAUTHORIZED),

    // Request and Input Data Errors
    INVALID_REQUEST(3001, "Dữ liệu yêu cầu không hợp lệ", HttpStatus.BAD_REQUEST),
    MISSING_REQUIRED_FIELDS(3002, "Thiếu các trường bắt buộc", HttpStatus.BAD_REQUEST),
    DATA_FORMAT_ERROR(3003, "Định dạng dữ liệu không hợp lệ", HttpStatus.BAD_REQUEST),
    UNSUPPORTED_MEDIA_TYPE(3004, "Loại phương tiện không được hỗ trợ", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    RESOURCE_CONFLICT(3005, "Xung đột tài nguyên", HttpStatus.CONFLICT),
    RESOURCE_NOT_FOUND(3006, "Không tìm thấy tài nguyên", HttpStatus.NOT_FOUND),

    // Document and File Errors
    INVALID_LOCATION_TYPE(4000, "Loại vị trí không hợp lệ", HttpStatus.BAD_REQUEST),
    DOCUMENT_NOT_FOUND(4001, "Không tìm thấy tài liệu", HttpStatus.NOT_FOUND),
    DUPLICATE_DOCUMENT(4002, "Tài liệu đã tồn tại", HttpStatus.BAD_REQUEST),
    FILE_UPLOAD_FAILED(4003, "Tải lên tệp thất bại", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_NOT_FOUND(4004, "Không tìm thấy tệp", HttpStatus.NOT_FOUND),
    FILE_TOO_LARGE(4005, "Tệp quá lớn", HttpStatus.PAYLOAD_TOO_LARGE),
    FILE_FORMAT_UNSUPPORTED(4006, "Định dạng tệp không được hỗ trợ", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    DOCUMENT_TYPE_NOT_FOUND(4007, "Không tìm thấy loại tài liệu", HttpStatus.NOT_FOUND),
    DOCUMENT_ALREADY_FAVORITE(4008, "Tài liệu đã được đánh dấu yêu thích", HttpStatus.CONFLICT),
    COURSE_NOT_FOUND(4009,"Không tìm thấy khóa học",HttpStatus.NOT_FOUND),
    // Warehouse Errors
    WAREHOUSE_NOT_FOUND(4009, "Không tìm thấy kho", HttpStatus.NOT_FOUND),
    INVALID_QUANTITY(4010, "Số lượng không thể âm", HttpStatus.BAD_REQUEST),
    RACK_NOT_FOUND(4011, "Không tìm thấy giá sách", HttpStatus.NOT_FOUND),
    LOCATION_NOT_FOUND(4012, "Không tìm thấy vị trí tài liệu trong giá sách", HttpStatus.NOT_FOUND),
    RACK_CAPACITY_EXCEEDED(4013, "Giá sách không có đủ dung lượng", HttpStatus.BAD_REQUEST),
    DEPARTMENT_NOT_FOUND(4014,"Không tìm thấy phòng ban",HttpStatus.NOT_FOUND),

    // Database Errors
    DATA_UNIQUE(5000,"Dữ liệu phải là duy nhất",HttpStatus.BAD_REQUEST),
    DATABASE_ERROR(5001, "Lỗi cơ sở dữ liệu", HttpStatus.INTERNAL_SERVER_ERROR),
    DATA_INTEGRITY_VIOLATION(5002, "Vi phạm tính toàn vẹn dữ liệu", HttpStatus.CONFLICT),
    TRANSACTION_FAILED(5003, "Giao dịch cơ sở dữ liệu thất bại", HttpStatus.INTERNAL_SERVER_ERROR),
    TRANSACTION_NOT_FOUND(5004, "Không tìm thấy giao dịch mượn", HttpStatus.NOT_FOUND),
    FINE_NOT_FOUND(5005, "Không tìm thấy tiền phạt", HttpStatus.NOT_FOUND),
    POLICY_NOT_FOUND(5006, "Không tìm thấy chính sách mượn", HttpStatus.NOT_FOUND),

    // System Errors
    SERVER_ERROR(6001, "Lỗi máy chủ nội bộ", HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE(6002, "Dịch vụ tạm thời không khả dụng", HttpStatus.SERVICE_UNAVAILABLE),
    GATEWAY_TIMEOUT(6003, "Hết thời gian chờ cổng", HttpStatus.GATEWAY_TIMEOUT),
    CONFIGURATION_ERROR(6004, "Lỗi cấu hình hệ thống", HttpStatus.INTERNAL_SERVER_ERROR),
    UNKNOWN_ERROR(6005, "Đã xảy ra lỗi không xác định", HttpStatus.INTERNAL_SERVER_ERROR),
    FORBIDDEN(6006,"Truy cập bị cấm",HttpStatus.FORBIDDEN),
    // Notification Errors
    NOTIFICATION_NOT_FOUND(7001, "Không tìm thấy thông báo", HttpStatus.NOT_FOUND),
    NOTIFICATION_SENDING_ERROR(7002, "Lỗi gửi thông báo", HttpStatus.INTERNAL_SERVER_ERROR),
    NOTIFICATION_INVALID_STATUS(7003, "Thông báo có trạng thái không hợp lệ", HttpStatus.BAD_REQUEST),
    // Review Errors
    REVIEW_ALREADY_EXISTS(8001, "Đánh giá đã tồn tại", HttpStatus.CONFLICT),
    REVIEW_NOT_FOUND(8002, "Không tìm thấy đánh giá", HttpStatus.NOT_FOUND),
    USER_HAS_NOT_BORROWED_DOCUMENT(8003, "Người dùng chưa mượn tài liệu này", HttpStatus.FORBIDDEN),
    //
    CLOUDINARY_UPLOAD_FAILED(9001, "Tải lên Cloudinary thất bại", HttpStatus.INTERNAL_SERVER_ERROR),
    //File system
    UNSUPPORTED_FILE(10001,"Tệp không được hỗ trợ",HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    FILE_PROCESSING_ERROR(10002,"Lỗi xử lý tệp",HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    INVALID_STATUS(10003,"Trạng thái không hợp lệ" ,HttpStatus.BAD_REQUEST ),
    INTERNAL_SERVER_ERROR(10004,"Lỗi máy chủ nội bộ" ,HttpStatus.INTERNAL_SERVER_ERROR ),
    LICENSE_NOT_FOUND(10005,"Không tìm thấy giấy phép" ,HttpStatus.NOT_FOUND ),
    LICENSE_EXPIRED(10006,"Giấy phép đã hết hạn" ,HttpStatus.UNAUTHORIZED ),
    DECRYPTION_ERROR(10007,"Lỗi giải mã" ,HttpStatus.INTERNAL_SERVER_ERROR ),
    INVALID_CONTENT_KEY(10008,"Khóa nội dung không hợp lệ" ,HttpStatus.UNAUTHORIZED ),
    SESSION_CREATION_ERROR(10009,"Lỗi tạo phiên" ,HttpStatus.INTERNAL_SERVER_ERROR ),
    DEVICE_LIMIT_EXCEEDED(100010,"Vượt quá số thiết bị được phép", HttpStatus.TOO_MANY_REQUESTS );;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;
}
