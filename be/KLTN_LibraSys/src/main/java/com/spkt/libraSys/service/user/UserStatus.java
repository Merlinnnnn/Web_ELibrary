package com.spkt.libraSys.service.user;

public enum UserStatus {
    PENDING,     // Đăng ký nhưng chưa được xác minh
    ACTIVE,       // Tài khoản đang hoạt động
    LOCKED,       // Tài khoản bị khóa (do vi phạm, bảo mật, hoặc các lý do khác)
    DELETED       // Tài khoản đã bị xóa (đánh dấu là không còn hoạt động)
}
