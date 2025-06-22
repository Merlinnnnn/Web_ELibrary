package com.spkt.libraSys.service.user;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserUpdateRequest {

    @NotBlank(message = "FIRST_NAME_CANNOT_BE_BLANK")
    String firstName;

    @NotBlank(message = "LAST_NAME_CANNOT_BE_BLANK")
    String lastName;

    @Past(message = "DOB_MUST_BE_IN_THE_PAST")
    LocalDate dob;

    @NotBlank(message = "PHONE_NUMBER_CANNOT_BE_BLANK")
    @Pattern(regexp = "^(\\+84|0)[0-9]{9,10}$", message = "INVALID_PHONE_NUMBER_FORMAT")
    String phoneNumber;

    @NotBlank(message = "ADDRESS_CANNOT_BE_BLANK")
    String address;


    // Cập nhật khóa học và chuyên ngành nếu có
    @Min(value = 1, message = "STUDENT_BATCH_INVALID")
    int studentBatch;

    @NotBlank(message = "MAJOR_CODE_CANNOT_BE_BLANK")
    String majorCode;
}
