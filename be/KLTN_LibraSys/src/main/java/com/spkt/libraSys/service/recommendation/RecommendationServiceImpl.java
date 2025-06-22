package com.spkt.libraSys.service.recommendation;


import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;
import com.spkt.libraSys.service.PageDTO;
import com.spkt.libraSys.service.access.AuthService;
import com.spkt.libraSys.service.document.DocumentEntity;
import com.spkt.libraSys.service.document.DocumentMapper;
import com.spkt.libraSys.service.document.DocumentRepository;
import com.spkt.libraSys.service.document.DocumentResponseDto;
import com.spkt.libraSys.service.document.course.CourseEntity;
import com.spkt.libraSys.service.document.programClass.ProgramClassEntity;
import com.spkt.libraSys.service.document.programClass.ProgramClassRepository;
import com.spkt.libraSys.service.user.UserEntity;
import com.spkt.libraSys.service.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final UserRepository userRepository;
    private final ProgramClassRepository programClassRepository;
    private final DocumentRepository documentRepository;
    private final DocumentMapper documentMapper;
    private final AuthService authService;

    @Override
    public PageDTO<DocumentResponseDto> getRecommendedDocumentsForCurrentUser(Pageable pageable) {
        UserEntity user = authService.getCurrentUser();

        if (user.getStudentBatch() == 0 || user.getMajorCode() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Người dùng chưa khai báo đủ studentBatch hoặc majorCode.");
        }
        int currentSemester = determineCurrentSemester();
        String currentSchoolYear = determineCurrentSchoolYear();
        
        // Add debug logs
        log.info("Searching program classes with params:");
        log.info("studentBatch: {}", user.getStudentBatch());
        log.info("majorCode: {}", user.getMajorCode());
        log.info("year: {}", currentSchoolYear);
        log.info("semester: {}", currentSemester);
        
        List<ProgramClassEntity> matchedClasses = programClassRepository
                .findByStudentBatchAndMajorCodeAndYearAndSemester(
                        user.getStudentBatch(), user.getMajorCode(), currentSchoolYear, currentSemester);
        
        // Log search results
        log.info("Found {} matched classes", matchedClasses.size());
        if (matchedClasses.isEmpty()) {
            log.warn("No program classes found for the given criteria");
        } else {
            matchedClasses.forEach(c -> 
                log.info("Found class: id={}, year={}, semester={}, batch={}, major={}", 
                    c.getClassId(), c.getYear(), c.getSemester(), c.getStudentBatch(), c.getMajorCode())
            );
        }

        Set<CourseEntity> courses = matchedClasses.stream()
                .flatMap(c -> c.getCourses().stream())
                .collect(Collectors.toSet());

        if (courses.isEmpty()) {
            return new PageDTO<>(Page.empty(pageable));
        }

        Page<DocumentEntity> documents = documentRepository.findByCoursesIn(courses,pageable);
        return new PageDTO<>(documents.map(documentMapper::toDocumentResponse));
    }

    private int determineCurrentSemester() {
        int month = LocalDate.now().getMonthValue();
        return (month >= 6 && month <= 11) ? 1 : 2; // Tháng 6 - 11 là HK1, còn lại là HK2
    }

    private String determineCurrentSchoolYear() {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        return (month >= 6) ? year + "-" + (year + 1) : (year - 1) + "-" + year;
    }

}