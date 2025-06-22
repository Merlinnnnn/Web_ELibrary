package com.spkt.libraSys.service.document.programClass;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProgramClassRepository extends JpaRepository<ProgramClassEntity,Long> {
    boolean existsByStudentBatchAndMajorCodeAndCourses_CourseCode(
             int studentBatch, String majorCode, String courseCode);
    List<ProgramClassEntity> findByStudentBatchAndMajorCodeAndYearAndSemester(int studentBatch, String majorCode, String year, int semester);
}
