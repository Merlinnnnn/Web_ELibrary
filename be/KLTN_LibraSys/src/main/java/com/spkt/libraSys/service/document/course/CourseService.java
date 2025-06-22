package com.spkt.libraSys.service.document.course;

import com.spkt.libraSys.service.PageDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

/**
 * Interface providing methods for course management in the system.
 * These methods include creating, updating, retrieving, deleting courses, and importing courses from Excel files.
 */
public interface CourseService {

    /**
     * Create a new course.
     *
     * @param request Course creation request information (course name, description, etc.).
     * @return Response containing details of the newly created course.
     */
    CourseResponse createCourse(CourseRequest request);

    /**
     * Update course information.
     *
     * @param id      ID of the course to update.
     * @param request Course update request information.
     * @return Response containing details of the updated course.
     */
    CourseResponse updateCourse(Long id, CourseRequest request);

    /**
     * Get list of all courses with pagination support.
     *
     * @param pageable Pagination parameters for retrieving the course list.
     * @return Paginated list of courses.
     */
    PageDTO<CourseResponse> getAllCourses(Pageable pageable);

    /**
     * Get detailed information of a course by ID.
     *
     * @param id ID of the course to retrieve.
     * @return Response containing detailed course information.
     */
    CourseResponse getCourseById(Long id);

    /**
     * Delete a course by ID.
     *
     * @param id ID of the course to delete.
     */
    void deleteCourse(Long id);

    /**
     * Create courses from an Excel file.
     *
     * @param file Excel file containing course data.
     */
    void createCoursesFromExcel(MultipartFile file);
}
