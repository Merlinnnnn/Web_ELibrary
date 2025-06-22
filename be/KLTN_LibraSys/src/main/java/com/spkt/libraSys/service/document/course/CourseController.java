package com.spkt.libraSys.service.document.course;

import com.spkt.libraSys.exception.ApiResponse;
import com.spkt.libraSys.service.PageDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller handling requests related to courses.
 * Provides APIs for creating, updating, retrieving, deleting, and uploading course lists from Excel.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/courses")
public class CourseController {

    private final CourseService courseService;

    /**
     * Create a new course.
     *
     * @param request Course information to create.
     * @return ResponseEntity containing the created course information.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CourseResponse>> createCourse(@Valid @RequestBody CourseRequest request) {
        CourseResponse response = courseService.createCourse(request);
        ApiResponse<CourseResponse> apiResponse = ApiResponse.<CourseResponse>builder()
                .message("Khóa học đã được tạo thành công")
                .data(response)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    /**
     * Update course information by ID.
     *
     * @param id ID of the course to update.
     * @param request Updated information for the course.
     * @return ResponseEntity containing the updated course information.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> updateCourse(@PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        CourseResponse response = courseService.updateCourse(id, request);
        ApiResponse<CourseResponse> apiResponse = ApiResponse.<CourseResponse>builder()
                .message("Khóa học đã được cập nhật thành công")
                .data(response)
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Get list of all courses with pagination.
     *
     * @param pageable Pagination parameters for the results.
     * @return ResponseEntity containing the list of courses.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageDTO<CourseResponse>>> getAllCourses(Pageable pageable) {
        PageDTO<CourseResponse> response = courseService.getAllCourses(pageable);
        ApiResponse<PageDTO<CourseResponse>> apiResponse = ApiResponse.<PageDTO<CourseResponse>>builder()
                .message("Danh sách khóa học đã được truy xuất thành công")
                .data(response)
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Get detailed information of a course by ID.
     *
     * @param id ID of the course to retrieve.
     * @return ResponseEntity containing the course details.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> getCourseById(@PathVariable Long id) {
        CourseResponse response = courseService.getCourseById(id);
        ApiResponse<CourseResponse> apiResponse = ApiResponse.<CourseResponse>builder()
                .message("Thông tin khóa học đã được truy xuất thành công")
                .data(response)
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Delete a course by ID.
     *
     * @param id ID of the course to delete.
     * @return ResponseEntity indicating successful deletion.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .message("Khóa học đã được xóa thành công")
                .build();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(apiResponse);
    }

    /**
     * Create multiple courses from Excel file.
     *
     * @param file Excel file containing the list of courses to create.
     * @return ResponseEntity indicating successful file upload.
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Void>> createCoursesFromExcel(@RequestParam("file") MultipartFile file) {
        courseService.createCoursesFromExcel(file);
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .message("Tệp Excel đã được tải lên và xử lý thành công")
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }
}
