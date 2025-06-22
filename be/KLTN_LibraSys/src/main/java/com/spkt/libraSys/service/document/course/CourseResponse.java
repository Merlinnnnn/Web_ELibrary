package com.spkt.libraSys.service.document.course;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseResponse {

    Long courseId;
    String courseCode; // Mã môn học
    String courseName; // Tên môn học
    String description; // Mô tả môn học
}
