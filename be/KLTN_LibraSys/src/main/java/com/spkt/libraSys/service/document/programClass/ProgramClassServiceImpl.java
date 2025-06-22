package com.spkt.libraSys.service.document.programClass;


import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;
import com.spkt.libraSys.service.document.course.CourseEntity;
import com.spkt.libraSys.service.document.course.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProgramClassServiceImpl implements ProgramClassService {

    private final ProgramClassRepository programClassRepository;
    //  private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;


    @Override
    @Transactional
    public ProgramClassUploadResult saveProgramClassesFromExcel(MultipartFile file) {
        int totalRows = 0, inserted = 0, skipped = 0, duplicated = 0;

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            Map<String, ProgramClassEntity> classMap = new HashMap<>();

            Iterator<Row> rows = sheet.iterator();
            if (rows.hasNext()) rows.next(); // Bỏ qua tiêu đề

            while (rows.hasNext()) {
                Row row = rows.next();
                totalRows++;

                try {
                    String cohort = getCellStringValue(row.getCell(0)).replace("K", "").trim();
                    String majorCode = getCellStringValue(row.getCell(1)).trim();
                    String year = getCellStringValue(row.getCell(2)).trim();
                    int semester = Integer.parseInt(getCellStringValue(row.getCell(3)).trim());
                    String courseCode = getCellStringValue(row.getCell(4)).trim();
                    String courseName = getCellStringValue(row.getCell(5)).trim();
                    String description = getCellStringValue(row.getCell(6)).trim();

                    int batch = Integer.parseInt(cohort);

                    CourseEntity course = courseRepository.findByCourseCode(courseCode)
                            .orElseGet(() -> courseRepository.save(CourseEntity.builder()
                                    .courseCode(courseCode)
                                    .courseName(courseName)
                                    .description(description)
                                    .build()));

                    // Check trùng
                    boolean exists = programClassRepository
                            .existsByStudentBatchAndMajorCodeAndCourses_CourseCode(batch, majorCode, courseCode);

                    if (exists) {
                        duplicated++;
                        continue;
                    }

                    // Key để gom lớp
                    String key = year + "-" + semester + "-" + batch + "-" + majorCode;

                    ProgramClassEntity programClassEntity = classMap.computeIfAbsent(key, k ->
                            ProgramClassEntity.builder()
                                    .year(year)
                                    .semester(semester)
                                    .studentBatch(batch)
                                    .majorCode(majorCode)
                                    .courses(new HashSet<>())
                                    .build()
                    );

                    programClassEntity.getCourses().add(course);
                    inserted++;

                } catch (Exception e) {
                    skipped++;
                    log.warn("❌ Bỏ qua dòng {} do lỗi: {}", row.getRowNum() + 1, e.getMessage());
                }
            }

            programClassRepository.saveAll(classMap.values());

        } catch (IOException e) {
            throw new AppException(ErrorCode.SERVER_ERROR, "Không thể xử lý file Excel: " + e.getMessage());
        }

        return ProgramClassUploadResult.builder()
                .totalRows(totalRows)
                .inserted(inserted)
                .skipped(skipped)
                .duplicated(duplicated)
                .build();
    }




    private boolean isCellNotEmpty(Cell cell) {
        return cell != null && cell.getCellType() != CellType.BLANK && !getCellStringValue(cell).trim().isEmpty();
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                } else {
                    double numericValue = cell.getNumericCellValue();
                    return numericValue == (long) numericValue ? String.valueOf((long) numericValue) : String.valueOf(numericValue);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
                return evaluator.evaluate(cell).formatAsString();
            default:
                return "";
        }
    }

    public ProgramClassResponse getProgramClassById(Long id) {
        ProgramClassEntity programClassEntity = programClassRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "ProgramClass không tồn tại"));

        return convertToResponse(programClassEntity);
    }

    @Override
    public Page<ProgramClassResponse> getAllProgramClasses(Pageable pageable) {
        return null;
    }

    public Page<ProgramClassResponse> getAllProgramClassEntityes(Pageable pageable) {
        Page<ProgramClassEntity> programClasses = programClassRepository.findAll(pageable);
        return programClasses.map(this::convertToResponse);
    }

    public ProgramClassResponse createProgramClass(ProgramClassResponse request) {
        ProgramClassEntity programClassEntity = new ProgramClassEntity();
        programClassEntity.setYear(request.getYear());
        programClassEntity.setSemester(request.getSemester());
        programClassEntity.setStudentBatch(request.getStudentBatch());
        // Add Department and Courses logic

        programClassEntity = programClassRepository.save(programClassEntity);
        return convertToResponse(programClassEntity);
    }

    public ProgramClassResponse updateProgramClass(Long id, ProgramClassResponse request) {
        ProgramClassEntity programClassEntity = programClassRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "ProgramClass không tồn tại"));

        programClassEntity.setYear(request.getYear());
        programClassEntity.setSemester(request.getSemester());
        programClassEntity.setStudentBatch(request.getStudentBatch());
        // Update Department and Courses logic

        programClassEntity = programClassRepository.save(programClassEntity);
        return convertToResponse(programClassEntity);
    }

    public void deleteProgramClass(Long id) {
        ProgramClassEntity programClassEntity = programClassRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "ProgramClass không tồn tại"));
        programClassRepository.delete(programClassEntity);
    }

    public void deleteProgramClasses(List<Long> ids) {
        List<ProgramClassEntity> programClassEntities = programClassRepository.findAllById(ids);
        if (programClassEntities.size() != ids.size()) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Một hoặc nhiều ProgramClass không tồn tại");
        }
        programClassRepository.deleteAll(programClassEntities);
    }

    private ProgramClassResponse convertToResponse(ProgramClassEntity programClassEntity) {
        return ProgramClassResponse.builder()
                .id(programClassEntity.getClassId())
                .year(programClassEntity.getYear())
                .semester(programClassEntity.getSemester())
                .studentBatch(programClassEntity.getStudentBatch())
               // .departmentName(programClassEntity.getDepartment().getDepartmentName())
                .courseCodes(programClassEntity.getCourses().stream().map(course -> course.getCourseCode()).collect(Collectors.toSet()))
                .build();
    }

}