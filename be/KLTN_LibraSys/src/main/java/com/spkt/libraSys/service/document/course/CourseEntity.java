package com.spkt.libraSys.service.document.course;

import com.spkt.libraSys.service.document.DocumentEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(name = "courses")
public class CourseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id", nullable = false, unique = true)
    Long courseId;

    @Column(name = "course_code", nullable = false, unique = true)
    String courseCode; // Mã môn học, ví dụ: "CS101"

    @Column(name = "course_name", nullable = false)
    String courseName;

    @Column(name = "description", length = 1000)
    String description; // Mô tả môn học

//    // Quan hệ Many-to-Many với StudentClass
//    @ManyToMany(mappedBy = "courses")
//    @ToString.Exclude
//    Set<ProgramClass> programClasses = new HashSet<>();

    // Quan hệ Many-to-Many với Document
    @ManyToMany(mappedBy = "courses")
    @Builder.Default
    Set<DocumentEntity> documents = new HashSet<>();
}
