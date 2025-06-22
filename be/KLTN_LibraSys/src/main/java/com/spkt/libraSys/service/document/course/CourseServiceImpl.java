package com.spkt.libraSys.service.document.course;

import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;
import com.spkt.libraSys.service.PageDTO;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourseServiceImpl implements CourseService {

    CourseRepository courseRepository;
    CourseMapper courseMapper;

    @Override
    public CourseResponse createCourse(CourseRequest request) {
        if (courseRepository.existsByCourseCode(request.getCourseCode())) {
            throw new AppException(ErrorCode.DUPLICATE_DOCUMENT,"Course code already exists " + request.getCourseCode());
        }

        CourseEntity course = courseMapper.toEntity(request);
        CourseEntity savedCourse = courseRepository.save(course);
        return courseMapper.toResponse(savedCourse);
    }

    @Override
    public CourseResponse updateCourse(Long id, CourseRequest request) {
        CourseEntity course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        course.setCourseCode(request.getCourseCode());
        course.setCourseName(request.getCourseName());
        course.setDescription(request.getDescription());

        CourseEntity updatedCourse = courseRepository.save(course);
        return courseMapper.toResponse(updatedCourse);
    }

    @Override
    public PageDTO<CourseResponse> getAllCourses(Pageable pageable) {
        Page<CourseEntity> coursePage = courseRepository.findAll(pageable);
        Page<CourseResponse> dtoPage = coursePage.map(courseMapper::toResponse);
        return new PageDTO<>(dtoPage);
    }

    @Override
    public CourseResponse getCourseById(Long id) {
        CourseEntity course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        return courseMapper.toResponse(course);
    }

    @Override
    public void deleteCourse(Long id) {
        courseRepository.deleteById(id);
    }

    /**
     * Process Excel file to create multiple courses.
     *
     * @param file Excel file containing the course list.
     */
    @Override
    public void createCoursesFromExcel(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            List<CourseEntity> courses = new ArrayList<>();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) { // Skip header row
                Row row = sheet.getRow(i);
                if (row == null) continue;

                CourseEntity course = CourseEntity.builder()
                        .courseCode(getCellValue(row.getCell(0)))
                        .courseName(getCellValue(row.getCell(1)))
                        .description(getCellValue(row.getCell(2)))
                        .build();

                courses.add(course);
            }

            courseRepository.saveAll(courses);
            log.info("Đã thêm {} khóa học từ file Excel", courses.size());

        } catch (Exception e) {
            log.error("Lỗi xử lý file Excel: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.SERVER_ERROR, "Không thể xử lý file Excel");
        }
    }

    /**
     * Get value from Excel cell, ensuring no null errors.
     *
     * @param cell Data cell in Excel.
     * @return Value as String.
     */
    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue()); // Convert number to string
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}
