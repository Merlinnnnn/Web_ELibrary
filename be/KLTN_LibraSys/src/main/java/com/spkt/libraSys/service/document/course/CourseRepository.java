package com.spkt.libraSys.service.document.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<CourseEntity,Long> {
    boolean existsByCourseCode(String courseCode);
    Optional<CourseEntity> findByCourseCode(String courseCode);
}
