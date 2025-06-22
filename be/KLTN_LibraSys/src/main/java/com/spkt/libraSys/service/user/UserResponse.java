package com.spkt.libraSys.service.user;

import com.spkt.libraSys.service.role.RoleResponse;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    String userId;
    String username;
    String firstName;
    String lastName;
    LocalDate dob;
    String phoneNumber;
    String address;
    LocalDate registrationDate;
    LocalDate expirationDate;
    int currentBorrowedCount;
    int maxBorrowLimit;
    List<String> roles;

    int studentBatch;
    String majorCode;
//    DepartmentResponse department;


    // Các trường trạng thái của tài khoản
    UserStatus isActive; // Trạng thái tài khoản: ACTIVE, DEACTIVATED, LOCKED
    String lockReason; // Lý do tài khoản bị khóa
}