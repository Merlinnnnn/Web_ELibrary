package com.spkt.libraSys.service.document.programClass;

import com.spkt.libraSys.service.document.course.CourseEntity;
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
@Entity(name = "program_classes")
public class ProgramClassEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "class_id", nullable = false, unique = true)
    Long classId;

    @Column(name = "year", nullable = false)
    String year; // Năm học, ví dụ: "2023-2024"

    @Column(name = "semester", nullable = false)
    int semester; // Học kỳ, ví dụ: 1, 2

    @Column(name = "student_batch", nullable = false)
    int studentBatch; // Khóa học, ví dụ: 21 (tương ứng với K21), 22 (tương ứng với K22)

    @Column(name = "major_code", nullable = false, length = 10)
    String majorCode;
    // Quan hệ Many-to-Many với Course
    @ManyToMany
    @JoinTable(
            name = "class_courses",
            joinColumns = @JoinColumn(name = "class_id"),
            inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    @Builder.Default
    Set<CourseEntity> courses = new HashSet<>();

    public void addCourse(CourseEntity course) {
        this.courses.add(course);              
    }

    public void removeCourse(CourseEntity course) {
        this.courses.remove(course);
    }
}