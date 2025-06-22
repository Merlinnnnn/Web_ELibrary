package com.spkt.libraSys.service.document.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CourseRequest {

    @NotBlank(message = "Course code is required")
   // @Size(max = 10, message = "Course code must not exceed 10 characters")
    String courseCode;

    @NotBlank(message = "Course name is required")
    @Size(max = 100, message = "Course name must not exceed 100 characters")
    String courseName;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    String description;
}
