package com.spkt.libraSys.service.document;

import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;
import com.spkt.libraSys.service.access.AuthService;
import com.spkt.libraSys.service.document.DigitalDocument.DigitalDocumentEntity;
import com.spkt.libraSys.service.document.DigitalDocument.DigitalDocumentRepository;
import com.spkt.libraSys.service.document.DocumentType.DocumentTypeEntity;
import com.spkt.libraSys.service.document.DocumentType.DocumentTypeRepository;
import com.spkt.libraSys.service.document.PhysicalDocument.PhysicalDocumentEntity;
import com.spkt.libraSys.service.document.PhysicalDocument.PhysicalDocumentRepository;
import com.spkt.libraSys.service.document.PhysicalDocument.PhysicalDocumentService;
import com.spkt.libraSys.service.document.briefDocs.DocumentTextExtractionService;
import com.spkt.libraSys.service.document.briefDocs.DocumentTextExtractionService;
import com.spkt.libraSys.service.document.course.CourseEntity;
import com.spkt.libraSys.service.document.course.CourseRepository;
import com.spkt.libraSys.service.document.documentValidation.DocumentValidationService;
import com.spkt.libraSys.service.document.upload.UploadEntity;
import com.spkt.libraSys.service.document.upload.UploadService;
import com.spkt.libraSys.service.document.viewer.ApprovalStatus;
import com.spkt.libraSys.service.role.RoleService;
import com.spkt.libraSys.service.user.UserEntity;
import com.spkt.libraSys.service.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.persistence.criteria.Join;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DocumentServiceImpl implements DocumentService {
    DocumentRepository documentRepository;
    DocumentValidationService validationService;
    UploadService uploadService;
     AuthService authService;
    DocumentMapper documentMapper;
    DigitalDocumentRepository digitalDocumentRepository;
    PhysicalDocumentRepository physicalDocumentRepository;
    DocumentTypeRepository documentTypeRepository;
    CourseRepository courseRepository;
    RoleService roleService;
    DocumentTextExtractionService DocumentTextExtractionService;
    private final UserRepository userRepository;

    // Phương thức bổ trợ để lấy danh sách Course theo ID
    private Set<CourseEntity> getCoursesByIds(Set<Long> courseIds) {
        return new HashSet<>(courseRepository.findAllById(courseIds));
    }

    private Set<DocumentTypeEntity> getDocumentTypes(Set<Long> documentTypeIds) {
        Set<DocumentTypeEntity> documentTypes = new HashSet<>(documentTypeRepository.findAllById(documentTypeIds));
        if (documentTypes.size() != documentTypeIds.size()) {
            throw new AppException(ErrorCode.DOCUMENT_TYPE_NOT_FOUND, "One or more DocumentTypeIds are invalid");
        }
        return documentTypes;
    }


    @Transactional
    public DocumentResponseDto createDocument(DocumentCreateRequest request) throws IOException {
        // lay thong tin user dang upload
        UserEntity user = authService.getCurrentUser();
        // 1️⃣ Kiểm tra dữ liệu đầu vào
        validationService.validateDocument(request);
        DocumentEntity document = documentMapper.toDocument(request);
        document.setApprovalStatus(ApprovalStatus.APPROVED);
        document.setDocumentCategory(DocumentCategory.PHYSICAL);
        document.setDocumentTypes(getDocumentTypes(request.getDocumentTypeIds()));
        if (request.getCourseIds() != null && !request.getCourseIds().isEmpty()) {
            document.setCourses(getCoursesByIds(request.getCourseIds()));
        }

        if(request.getCoverImage()!=null)
        {
            uploadService.uploadImage(request.getCoverImage(),document);
        }
        document =  documentRepository.save(document);
        //
        PhysicalDocumentEntity physicalDocument = PhysicalDocumentEntity.builder()
                .price(request.getPrice())
                .isbn(request.getIsbn())
                .quantity(request.getQuantity())
                .document(document)
                .build();
        physicalDocumentRepository.save(physicalDocument);
        document.setPhysicalDocument(physicalDocument);
        //
        DigitalDocumentEntity digitalDocument = null;
        if(request.getFiles()!=null){
            digitalDocument = new DigitalDocumentEntity();
            digitalDocument.setDocument(document);
            digitalDocument.setUser(user);
            // Upload từng file và liên kết với digitalDocument
            Set<UploadEntity> uploadEntities = uploadService.uploadFiles( request.getFiles() );
            digitalDocument.setUploads(uploadEntities);
            for (UploadEntity uploadEntity : uploadEntities) {
                uploadEntity.setDigitalDocument(digitalDocument); // Gán DigitalDocument vào từng UploadEntity
            }
            digitalDocumentRepository.save(digitalDocument);

            document.setDocumentCategory(DocumentCategory.BOTH);
            document.setDigitalDocument(digitalDocument);
        }
       // uploadService.uploadFiles(request, document);
        document =  documentRepository.save(document);
        documentRepository.flush();
        DocumentTextExtractionService.summarizeDoc(document.getDocumentId(),true);
        return documentMapper.toDocumentResponse(document);

    }


    @Transactional
    public DocumentResponseDto updateDocument(Long id, DocumentUpdateRequest request) {
        // 1️⃣ Tìm tài liệu theo ID
        DocumentEntity document = documentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));

        // 2️⃣ Kiểm tra dữ liệu đầu vào
        //validationService.validateDocument(request);

        // 3️⃣ Cập nhật thông tin cơ bản
        document.setDocumentName(request.getDocumentName());
        document.setAuthor(request.getAuthor());
        document.setPublisher(request.getPublisher());
        document.setPublishedDate(request.getPublishedDate());
        document.setLanguage(request.getLanguage());
        document.setDescription(request.getDescription());

        // 4️⃣ Cập nhật loại tài liệu & khóa học
        document.setDocumentTypes(getDocumentTypes(request.getDocumentTypeIds()));
        document.setCourses(getCoursesByIds(request.getCourseIds()));

        // 5️⃣ Cập nhật ảnh bìa nếu có
        if (request.getCoverImage() != null && !request.getCoverImage().isEmpty()) {
            uploadService.uploadImage(request.getCoverImage(), document);
        }

        // 6️⃣ Cập nhật thông tin vật lý nếu có
        PhysicalDocumentEntity physicalDocument = document.getPhysicalDocument();
        if (physicalDocument != null) {
            physicalDocument.setIsbn(request.getIsbn());
            physicalDocument.setQuantity(request.getQuantity());
            physicalDocumentRepository.save(physicalDocument);
        } else {
            // Nếu chưa có, tạo mới (nếu có thông tin vật lý)
            if (request.getIsbn() != null || request.getQuantity() > 0) {
                physicalDocument = PhysicalDocumentEntity.builder()
                        .isbn(request.getIsbn())
                        .quantity(request.getQuantity())
                        .document(document)
                        .build();
                physicalDocumentRepository.save(physicalDocument);
                document.setPhysicalDocument(physicalDocument);
            }
        }

        // 7️⃣ Cập nhật tài liệu số nếu có file
        if (request.getFiles() != null && !request.getFiles().isEmpty()) {
            DigitalDocumentEntity digitalDocument = document.getDigitalDocument();
            if (digitalDocument == null) {
                digitalDocument = new DigitalDocumentEntity();
                digitalDocument.setDocument(document);
                digitalDocument.setUser(authService.getCurrentUser());
            }

            // Upload file mới và gán
            Set<UploadEntity> uploads = uploadService.uploadFiles(request.getFiles());
            for (UploadEntity upload : uploads) {
                upload.setDigitalDocument(digitalDocument);
            }

            digitalDocument.getUploads().addAll(uploads);
            digitalDocumentRepository.save(digitalDocument);
            document.setDigitalDocument(digitalDocument);
        }

        // 8️⃣ Cập nhật loại tài liệu (PHYSICAL / DIGITAL / BOTH)
        boolean hasDigital = document.getDigitalDocument() != null;
        boolean hasPhysical = document.getPhysicalDocument() != null;
        if (hasDigital && hasPhysical) {
            document.setDocumentCategory(DocumentCategory.BOTH);
        } else if (hasDigital) {
            document.setDocumentCategory(DocumentCategory.DIGITAL);
        } else {
            document.setDocumentCategory(DocumentCategory.PHYSICAL);
        }

        // 9️⃣ Lưu tài liệu & trả kết quả
        document = documentRepository.save(document);
        DocumentTextExtractionService.summarizeDoc(document.getDocumentId(),true);

        return documentMapper.toDocumentResponse(document);
    }


    @Override
    public DocumentResponseDto getDocumentById(Long id) {
        DocumentEntity document = documentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Optional<UserEntity> userOptional = userRepository.findByUsername(username);
        if(document.getStatus().equals(DocumentStatus.ENABLED)){
            return documentMapper.toDocumentResponse(document);
        }
        if(userOptional.isPresent() && !roleService.isAdmin(userOptional.get())){
            //status = enable
            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS,"Bạn không có quyền truy cập tài liệu này");
        }

        return documentMapper.toDocumentResponse(document);
    }

    @Override
    public Page<DocumentResponseDto> getAllDocuments(Pageable pageable) {
        Specification<DocumentEntity> spec = Specification.where(null);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Optional<UserEntity> userOptional = userRepository.findByUsername(username);

        if(userOptional.isPresent() && !roleService.isAdmin(userOptional.get())){
            //status = enable
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), DocumentStatus.ENABLED));
            spec = spec.and((root, query, cb) -> cb.equal(root.get("approvalStatus"), ApprovalStatus.APPROVED));

        }
           Page<DocumentEntity> documents = documentRepository.findAll(spec, pageable);

        return documents.map(documentMapper::toDocumentResponse);
    }

    @Override
    public void deleteDocument(Long id) {
        DocumentEntity document = documentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));
        document.setStatus(DocumentStatus.DISABLED);
        documentRepository.save(document);

    }
    @Transactional
    public void deleteDocumentsByIds(List<Long> documentIds){
        List<DocumentEntity> documents = documentRepository.findAllById(documentIds);
        documents.forEach(document -> {
            document.setStatus(DocumentStatus.DISABLED);
                   });
        documentRepository.saveAll(documents);
    }

    @Override
    public void classifyDocument(Long id, String newTypeName) {

    }
    @Override
    public byte[] getDocumentPageContent(Long documentId, int pageNumber) {
        return new byte[0];
    }

    // Lấy danh sách tài liệu đang chờ duyệt
    public List<DocumentResponseDto> getPendingDocuments() {
//        return documentRepository.findByApprovalStatus(DocumentApprovalStatus.PENDING)
//                .stream().map(documentMapper::toDocumentResponse).collect(Collectors.toList());
        return null;
    }

    // Duyệt tài liệu
    public void approveDocument(Long documentId) {
//        DocumentEntity document = validationService.getDocumentById(documentId);
//        document.setApprovalStatus(DocumentApprovalStatus.APPROVED);
//        documentRepository.save(document);
    }

    // Từ chối tài liệu
    public void rejectDocument(Long documentId) {
//        DocumentEntity document = validationService.getDocumentById(documentId);
//        document.setApprovalStatus(DocumentApprovalStatus.REJECTED);
//        documentRepository.save(document);

    }

    @Override
    @Deprecated
    public DocumentResponseDto searchByTitle(String title) {
        List<DocumentEntity> documents = documentRepository.findByDocumentNameContainingIgnoreCase(title);
        if (documents.isEmpty()) {
            throw new AppException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        return documentMapper.toDocumentResponse(documents.get(0));
    }

    @Override
    public Page<DocumentResponseDto> searchByTitle(String title, Pageable pageable) {
        Specification<DocumentEntity> spec = (root, query, cb) -> 
            cb.like(cb.lower(root.get("documentName")), "%" + title.toLowerCase() + "%");

        // Add status filter for non-admin users
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Optional<UserEntity> userOptional = userRepository.findByUsername(username);

        if(userOptional.isPresent() && !roleService.isAdmin(userOptional.get())) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), DocumentStatus.ENABLED));
            spec = spec.and((root, query, cb) -> cb.equal(root.get("approvalStatus"), ApprovalStatus.APPROVED));

        }

        Page<DocumentEntity> documents = documentRepository.findAll(spec, pageable);
        return documents.map(documentMapper::toDocumentResponse);
    }

    @Override
    public Page<DocumentResponseDto> filterDocuments(DocumentFilterRequest filterRequest, Pageable pageable) {
        if (filterRequest == null) {
            return getAllDocuments(pageable);
        }

        Specification<DocumentEntity> spec = Specification.where(null);

        // Filter by document name (required)
        if (filterRequest.getDocumentName() != null && !filterRequest.getDocumentName().isEmpty()) {
            spec = spec.and((root, query, cb) -> 
                cb.like(cb.lower(root.get("documentName")), 
                    "%" + filterRequest.getDocumentName().toLowerCase() + "%"));
        }

        // Filter by author (required)
        if (filterRequest.getAuthor() != null && !filterRequest.getAuthor().isEmpty()) {
            spec = spec.and((root, query, cb) -> 
                cb.like(cb.lower(root.get("author")), 
                    "%" + filterRequest.getAuthor().toLowerCase() + "%"));
        }

        // Filter by publisher (required)
        if (filterRequest.getPublisher() != null && !filterRequest.getPublisher().isEmpty()) {
            spec = spec.and((root, query, cb) -> 
                cb.like(cb.lower(root.get("publisher")), 
                    "%" + filterRequest.getPublisher().toLowerCase() + "%"));
        }

        // Filter by language (required)
        if (filterRequest.getLanguage() != null && !filterRequest.getLanguage().isEmpty()) {
            spec = spec.and((root, query, cb) -> 
                cb.equal(root.get("language"), filterRequest.getLanguage()));
        }

        // Filter by document category (required)
        if (filterRequest.getDocumentCategory() != null) {
            spec = spec.and((root, query, cb) -> 
                cb.equal(root.get("documentCategory"), filterRequest.getDocumentCategory()));
        }

        // Filter by status (required)
        if (filterRequest.getStatus() != null) {
            spec = spec.and((root, query, cb) -> 
                cb.equal(root.get("status"), filterRequest.getStatus()));
        }

        // Filter by approval status (required)
        if (filterRequest.getApprovalStatus() != null) {
            spec = spec.and((root, query, cb) -> 
                cb.equal(root.get("approvalStatus"), filterRequest.getApprovalStatus()));
        }

        // Filter by published date range (required if either date is provided)
        if (filterRequest.getPublishedDateFrom() != null || filterRequest.getPublishedDateTo() != null) {
            spec = spec.and((root, query, cb) -> {
                if (filterRequest.getPublishedDateFrom() != null && filterRequest.getPublishedDateTo() != null) {
                    return cb.between(root.get("publishedDate"), 
                        filterRequest.getPublishedDateFrom(), 
                        filterRequest.getPublishedDateTo());
                } else if (filterRequest.getPublishedDateFrom() != null) {
                    return cb.greaterThanOrEqualTo(root.get("publishedDate"), 
                        filterRequest.getPublishedDateFrom());
                } else {
                    return cb.lessThanOrEqualTo(root.get("publishedDate"), 
                        filterRequest.getPublishedDateTo());
                }
            });
        }

        // Filter by courses (required if provided)
        if (filterRequest.getCourseIds() != null && !filterRequest.getCourseIds().isEmpty()) {
            spec = spec.and((root, query, cb) -> {
                Join<DocumentEntity, CourseEntity> courseJoin = root.join("courses");
                return courseJoin.get("courseId").in(filterRequest.getCourseIds());
            });
        }

        // Filter by document types (required if provided)
        if (filterRequest.getDocumentTypeIds() != null && !filterRequest.getDocumentTypeIds().isEmpty()) {
            spec = spec.and((root, query, cb) -> {
                Join<DocumentEntity, DocumentTypeEntity> typeJoin = root.join("documentTypes");
                return typeJoin.get("documentTypeId").in(filterRequest.getDocumentTypeIds());
            });
        }

        // Add status filter for non-admin users
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Optional<UserEntity> userOptional = userRepository.findByUsername(username);

        if(userOptional.isPresent() && !roleService.isAdmin(userOptional.get())) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), DocumentStatus.ENABLED));
            spec = spec.and((root, query, cb) -> cb.equal(root.get("approvalStatus"), ApprovalStatus.APPROVED));

        }

        Page<DocumentEntity> documentPage = documentRepository.findAll(spec, pageable);
        return documentPage.map(documentMapper::toDocumentResponse);
    }
}
