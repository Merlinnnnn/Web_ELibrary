package com.spkt.libraSys.service.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL) // Bỏ qua các giá trị null khi serialize
public class UserRequest {

    @NotBlank(message = "Username không được để trống")
    @Pattern(regexp = "^[A-Za-z0-9+_.-]+@(.+)$", message = "Email không hợp lệ")
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
//    @Pattern(
//        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$",
//        message = "Mật khẩu phải bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt"
//    )
    private String password;

    @Pattern(regexp = "^\\d{10,11}$", message = "Số điện thoại phải có 10-11 chữ số")
    private String phone;

    @Size(max = 100, message = "Họ không được quá 100 ký tự")
    private String firstName;

    @Size(max = 100, message = "Tên không được quá 100 ký tự")
    private String lastName;

    LocalDate dob;

    @NotBlank(message = "PHONE_NUMBER_CANNOT_BE_BLANK")
    String phoneNumber;

    @NotBlank(message = "ADDRESS_CANNOT_BE_BLANK")
    String address;

    LocalDate registrationDate; // Chỉ dùng khi update
    LocalDate expirationDate;   // Chỉ dùng khi update
}
