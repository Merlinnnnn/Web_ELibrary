package com.spkt.libraSys.service.document.documentValidation;

import com.spkt.libraSys.service.document.DocumentCreateRequest;
import com.spkt.libraSys.service.document.DocumentEntity;
import com.spkt.libraSys.service.document.DocumentType.DocumentTypeEntity;
import com.spkt.libraSys.service.document.DocumentType.DocumentTypeRepository;
import com.spkt.libraSys.service.document.DocumentUploadRequestDto;
import com.spkt.libraSys.service.document.PhysicalDocument.PhysicalRequestDto;
import com.spkt.libraSys.service.document.PhysicalDocument.PhysicalDocumentRepository;
import com.spkt.libraSys.service.document.course.CourseEntity;
import com.spkt.libraSys.service.document.course.CourseRepository;
import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DocumentValidationServiceImpl implements DocumentValidationService {
    PhysicalDocumentRepository physicalDocumentRepository;
    DocumentTypeRepository documentTypeRepository;
    CourseRepository courseRepository;

    public void validateDocument(PhysicalRequestDto request) {
        if (physicalDocumentRepository.existsByIsbn(request.getIsbn())) {
            throw new AppException(ErrorCode.DATA_UNIQUE, "ISBN already exists");
        }

        validateDocumentTypes(request.getDocumentTypeIds());
        validateCourses(request.getCourseIds());
    }
    public void validateDocument(DocumentUploadRequestDto request) {
        if (physicalDocumentRepository.existsByIsbn(request.getIsbn())) {
            throw new AppException(ErrorCode.DATA_UNIQUE, "ISBN already exists");
        }

        validateDocumentTypes(request.getDocumentTypeIds());
        validateCourses(request.getCourseIds());
    }
    public void validateDocument(DocumentCreateRequest request) {
        if (physicalDocumentRepository.existsByIsbn(request.getIsbn())) {
            throw new AppException(ErrorCode.DATA_UNIQUE, "ISBN already exists");
        }

        validateDocumentTypes(request.getDocumentTypeIds());
        validateCourses(request.getCourseIds());
    }

    private void validateDocumentTypes(Set<Long> documentTypeIds) {
        Set<DocumentTypeEntity> documentTypes = new HashSet<>(documentTypeRepository.findAllById(documentTypeIds));
        if (documentTypes.size() != documentTypeIds.size()) {
            throw new AppException(ErrorCode.DOCUMENT_TYPE_NOT_FOUND, "One or more DocumentTypeIds are invalid");
        }
    }

    private void validateCourses(Set<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return;
        }
        Set<CourseEntity> courses = new HashSet<>(courseRepository.findAllById(courseIds));
        if (courses.size() != courseIds.size()) {
            throw new AppException(ErrorCode.COURSE_NOT_FOUND, "One or more courseIds are invalid");
        }
    }

    public DocumentEntity getDocumentById(Long documentId) {
//        return physicalDocumentRepository.findById(documentId)
//                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND, "Document not found"));
    return null;
    }
}
