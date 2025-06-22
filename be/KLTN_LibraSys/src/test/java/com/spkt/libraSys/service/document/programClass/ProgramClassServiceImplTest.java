package com.spkt.libraSys.service.document.programClass;

import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.service.document.course.CourseEntity;
import com.spkt.libraSys.service.document.course.CourseRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgramClassServiceImplTest {

    @Mock
    private ProgramClassRepository programClassRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private ProgramClassServiceImpl programClassService;

    private ProgramClassEntity testProgramClass;
    private CourseEntity testCourse;
    private ProgramClassResponse testResponse;

    @BeforeEach
    void setUp() {
        // Setup test course
        testCourse = CourseEntity.builder()
                .courseId(1L)
                .courseCode("CS101")
                .courseName("Introduction to Programming")
                .description("Basic programming concepts")
                .build();

        // Setup test program class
        testProgramClass = ProgramClassEntity.builder()
                .classId(1L)
                .year("2023-2024")
                .semester(1)
                .studentBatch(21)
                .majorCode("CS")
                .courses(new HashSet<>(Collections.singletonList(testCourse)))
                .build();

        // Setup test response
        testResponse = ProgramClassResponse.builder()
                .id(1L)
                .year("2023-2024")
                .semester(1)
                .studentBatch(21)
                .courseCodes(new HashSet<>(Collections.singletonList("CS101")))
                .build();
    }

    @Test
    void getProgramClassById_Success() {
        when(programClassRepository.findById(1L)).thenReturn(Optional.of(testProgramClass));

        ProgramClassResponse result = programClassService.getProgramClassById(1L);

        assertNotNull(result);
        assertEquals(testResponse.getId(), result.getId());
        assertEquals(testResponse.getYear(), result.getYear());
        assertEquals(testResponse.getSemester(), result.getSemester());
        assertEquals(testResponse.getStudentBatch(), result.getStudentBatch());
        assertEquals(testResponse.getCourseCodes(), result.getCourseCodes());
    }

    @Test
    void getProgramClassById_NotFound() {
        when(programClassRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> programClassService.getProgramClassById(1L));
    }

    @Test
    void getAllProgramClassEntityes_Success() {
        List<ProgramClassEntity> programClasses = Collections.singletonList(testProgramClass);
        Page<ProgramClassEntity> programClassPage = new PageImpl<>(programClasses);
        Pageable pageable = PageRequest.of(0, 10);

        when(programClassRepository.findAll(pageable)).thenReturn(programClassPage);

        Page<ProgramClassResponse> result = programClassService.getAllProgramClassEntityes(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testResponse.getId(), result.getContent().get(0).getId());
    }

    @Test
    void createProgramClass_Success() {
        when(programClassRepository.save(any(ProgramClassEntity.class))).thenReturn(testProgramClass);

        ProgramClassResponse result = programClassService.createProgramClass(testResponse);

        assertNotNull(result);
        assertEquals(testResponse.getId(), result.getId());
        assertEquals(testResponse.getYear(), result.getYear());
        assertEquals(testResponse.getSemester(), result.getSemester());
        assertEquals(testResponse.getStudentBatch(), result.getStudentBatch());
    }

    @Test
    void updateProgramClass_Success() {
        when(programClassRepository.findById(1L)).thenReturn(Optional.of(testProgramClass));
        when(programClassRepository.save(any(ProgramClassEntity.class))).thenReturn(testProgramClass);

        ProgramClassResponse result = programClassService.updateProgramClass(1L, testResponse);

        assertNotNull(result);
        assertEquals(testResponse.getId(), result.getId());
        assertEquals(testResponse.getYear(), result.getYear());
        assertEquals(testResponse.getSemester(), result.getSemester());
        assertEquals(testResponse.getStudentBatch(), result.getStudentBatch());
    }

    @Test
    void updateProgramClass_NotFound() {
        when(programClassRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> programClassService.updateProgramClass(1L, testResponse));
    }

    @Test
    void deleteProgramClass_Success() {
        when(programClassRepository.findById(1L)).thenReturn(Optional.of(testProgramClass));
        doNothing().when(programClassRepository).delete(testProgramClass);

        assertDoesNotThrow(() -> programClassService.deleteProgramClass(1L));
        verify(programClassRepository).delete(testProgramClass);
    }

    @Test
    void deleteProgramClass_NotFound() {
        when(programClassRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> programClassService.deleteProgramClass(1L));
    }

    @Test
    void deleteProgramClasses_Success() {
        List<Long> ids = Arrays.asList(1L, 2L);
        List<ProgramClassEntity> programClasses = Arrays.asList(
            testProgramClass,
            ProgramClassEntity.builder().classId(2L).build()
        );

        when(programClassRepository.findAllById(ids)).thenReturn(programClasses);
        doNothing().when(programClassRepository).deleteAll(programClasses);

        assertDoesNotThrow(() -> programClassService.deleteProgramClasses(ids));
        verify(programClassRepository).deleteAll(programClasses);
    }

    @Test
    void deleteProgramClasses_NotFound() {
        List<Long> ids = Arrays.asList(1L, 2L);
        when(programClassRepository.findAllById(ids)).thenReturn(Collections.singletonList(testProgramClass));

        assertThrows(AppException.class, () -> programClassService.deleteProgramClasses(ids));
    }

    @Test
    void saveProgramClassesFromExcel_Success() throws IOException {
        // Create test Excel file
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Program Classes");
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Cohort");
            headerRow.createCell(1).setCellValue("Major Code");
            headerRow.createCell(2).setCellValue("Year");
            headerRow.createCell(3).setCellValue("Semester");
            headerRow.createCell(4).setCellValue("Course Code");
            headerRow.createCell(5).setCellValue("Course Name");
            headerRow.createCell(6).setCellValue("Description");

            // Create data row
            Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue("K21");
            dataRow.createCell(1).setCellValue("CS");
            dataRow.createCell(2).setCellValue("2023-2024");
            dataRow.createCell(3).setCellValue("1");
            dataRow.createCell(4).setCellValue("CS101");
            dataRow.createCell(5).setCellValue("Introduction to Programming");
            dataRow.createCell(6).setCellValue("Basic programming concepts");

            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            byte[] excelBytes = outputStream.toByteArray();

            // Create MultipartFile
            MockMultipartFile file = new MockMultipartFile(
                "file",
                "program_classes.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                excelBytes
            );

            // Mock repository responses
            when(courseRepository.findByCourseCode("CS101")).thenReturn(Optional.empty());
            when(courseRepository.save(any(CourseEntity.class))).thenReturn(testCourse);
            when(programClassRepository.existsByStudentBatchAndMajorCodeAndCourses_CourseCode(21, "CS", "CS101"))
                .thenReturn(false);
            when(programClassRepository.saveAll(any())).thenReturn(Collections.singletonList(testProgramClass));

            // Execute test
            ProgramClassUploadResult result = programClassService.saveProgramClassesFromExcel(file);

            // Verify results
            assertNotNull(result);
            assertEquals(1, result.getTotalRows());
            assertEquals(1, result.getInserted());
            assertEquals(0, result.getSkipped());
            assertEquals(0, result.getDuplicated());
        }
    }

    @Test
    void saveProgramClassesFromExcel_DuplicateEntry() throws IOException {
        // Create test Excel file with duplicate entry
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Program Classes");
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Cohort");
            headerRow.createCell(1).setCellValue("Major Code");
            headerRow.createCell(2).setCellValue("Year");
            headerRow.createCell(3).setCellValue("Semester");
            headerRow.createCell(4).setCellValue("Course Code");
            headerRow.createCell(5).setCellValue("Course Name");
            headerRow.createCell(6).setCellValue("Description");

            // Create data row
            Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue("K21");
            dataRow.createCell(1).setCellValue("CS");
            dataRow.createCell(2).setCellValue("2023-2024");
            dataRow.createCell(3).setCellValue("1");
            dataRow.createCell(4).setCellValue("CS101");
            dataRow.createCell(5).setCellValue("Introduction to Programming");
            dataRow.createCell(6).setCellValue("Basic programming concepts");

            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            byte[] excelBytes = outputStream.toByteArray();

            // Create MultipartFile
            MockMultipartFile file = new MockMultipartFile(
                "file",
                "program_classes.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                excelBytes
            );

            // Mock repository responses
            when(courseRepository.findByCourseCode("CS101")).thenReturn(Optional.of(testCourse));
            when(programClassRepository.existsByStudentBatchAndMajorCodeAndCourses_CourseCode(21, "CS", "CS101"))
                .thenReturn(true);

            // Execute test
            ProgramClassUploadResult result = programClassService.saveProgramClassesFromExcel(file);

            // Verify results
            assertNotNull(result);
            assertEquals(1, result.getTotalRows());
            assertEquals(0, result.getInserted());
            assertEquals(0, result.getSkipped());
            assertEquals(1, result.getDuplicated());
        }
    }
} 