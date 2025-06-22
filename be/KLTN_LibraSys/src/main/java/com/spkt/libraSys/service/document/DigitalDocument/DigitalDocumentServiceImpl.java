package com.spkt.libraSys.service.document.DigitalDocument;

import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;
import com.spkt.libraSys.service.PageDTO;
import com.spkt.libraSys.service.access.AuthService;
import com.spkt.libraSys.service.document.DigitalDocument.AccessRequest.AccessRequestEntity;
import com.spkt.libraSys.service.document.DigitalDocument.AccessRequest.AccessRequestRepository;
import com.spkt.libraSys.service.document.DocumentEntity;
import com.spkt.libraSys.service.document.DocumentFilterRequest;
import com.spkt.libraSys.service.document.DocumentRepository;
import com.spkt.libraSys.service.document.DocumentType.DocumentTypeEntity;
import com.spkt.libraSys.service.document.DocumentType.DocumentTypeRepository;
import com.spkt.libraSys.service.document.briefDocs.DocumentTextExtractionService;
import com.spkt.libraSys.service.document.course.CourseEntity;
import com.spkt.libraSys.service.document.course.CourseRepository;
import com.spkt.libraSys.service.document.upload.UploadEntity;
import com.spkt.libraSys.service.document.upload.UploadService;
import com.spkt.libraSys.service.document.viewer.ApprovalStatus;
import com.spkt.libraSys.service.role.RoleService;
import com.spkt.libraSys.service.user.UserEntity;
import com.spkt.libraSys.service.user.UserRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DigitalDocumentServiceImpl implements DigitalDocumentService {

    DigitalDocumentRepository digitalDocumentRepository;

    DocumentRepository documentRepository;

    UploadService uploadService;

    DigitalDocumentMapper digitalDocumentMapper;

    AuthService authService;

    UserRepository userRepository;

    RoleService roleService;

    DocumentTextExtractionService documentTextExtractionService;

    DocumentTypeRepository documentTypeRepository;

    CourseRepository courseRepository;
    private final AccessRequestRepository accessRequestRepository;


    @Transactional
    @Override
    public DigitalDocumentResponseDto createDigitalDocument(DigitalDocumentRequestDto request) throws IOException {
        UserEntity userCurr = authService.getCurrentUser();
        DocumentEntity documentEntity = digitalDocumentMapper.toDocumentEntity(request);

        // Set document types and courses
        if (request.getDocumentTypeIds() != null && !request.getDocumentTypeIds().isEmpty()) {
            Set<DocumentTypeEntity> documentTypes = new HashSet<>(documentTypeRepository.findAllById(request.getDocumentTypeIds()));
            if (documentTypes.size() != request.getDocumentTypeIds().size()) {
                throw new AppException(ErrorCode.DOCUMENT_TYPE_NOT_FOUND, "One or more DocumentTypeIds are invalid");
            }
            documentEntity.setDocumentTypes(documentTypes);
        }

        if (request.getCourseIds() != null && !request.getCourseIds().isEmpty()) {
            Set<CourseEntity> courses = new HashSet<>(courseRepository.findAllById(request.getCourseIds()));
            if (courses.size() != request.getCourseIds().size()) {
                throw new AppException(ErrorCode.COURSE_NOT_FOUND, "One or more CourseIds are invalid");
            }
            documentEntity.setCourses(courses);
        }

        // Xử lý coverImage
        if (request.getCoverImage() != null && !request.getCoverImage().isEmpty()) {
            uploadService.uploadImage(request.getCoverImage(), documentEntity);
        }

        // Lưu DocumentEntity
        documentEntity = documentRepository.save(documentEntity);

        // Tạo DigitalDocumentEntity
        DigitalDocumentEntity digitalDocument = new DigitalDocumentEntity();
        digitalDocument.setDocument(documentEntity);
        digitalDocument.setUser(userCurr);

        // Lưu DigitalDocumentEntity trước để có digitalDocumentId
        digitalDocument = digitalDocumentRepository.save(digitalDocument);

        // Xử lý upload files
        List<MultipartFile> files = request.getFiles();
        Set<UploadEntity> uploadEntities = new HashSet<>();
        if (files != null && !files.isEmpty()) {
            uploadEntities = uploadService.uploadFiles(files, digitalDocument);
            digitalDocument.setUploads(uploadEntities);
        }

        // Cập nhật DigitalDocumentEntity
        digitalDocumentRepository.save(digitalDocument);
        documentTextExtractionService.summarizeDoc(documentEntity.getDocumentId(), false);
        // Trả về response
        return digitalDocumentMapper.toResponseDto(digitalDocument);
    }

    @Override
    public DigitalDocumentResponseDto getDigitalDocument(Long digitalDocumentId) {
        DigitalDocumentEntity digitalDocument = digitalDocumentRepository.findById(digitalDocumentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND,"Digital document not found"));

        return digitalDocumentMapper.toResponseDto(digitalDocument);
    }

    @Override
    public Page<DigitalDocumentResponseDto> getAllDigitalDocuments(Pageable pageable) {
        return digitalDocumentRepository.findAll(pageable)
                .map(digitalDocumentMapper::toResponseDto);
    }

    @Transactional
    @Override
    public DigitalDocumentResponseDto updateDigitalDocument(Long digitalDocumentId, DigitalDocumentRequestDto requestDto) throws IOException {
        DigitalDocumentEntity digitalDocument = digitalDocumentRepository.findById(digitalDocumentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND,"Digital document not found"));

        DocumentEntity document = digitalDocument.getDocument();

        // Cập nhật thông tin tài liệu nếu có thay đổi
        if (requestDto.getDocumentName() != null) document.setDocumentName(requestDto.getDocumentName());
        if (requestDto.getAuthor() != null) document.setAuthor(requestDto.getAuthor());
        if (requestDto.getPublisher() != null) document.setPublisher(requestDto.getPublisher());
        if (requestDto.getDescription() != null) document.setDescription(requestDto.getDescription());

        // Cập nhật ảnh bìa nếu có
        if (requestDto.getCoverImage() != null && !requestDto.getCoverImage().isEmpty()) {
            uploadService.uploadImage(requestDto.getCoverImage(), document);
        }

        // Lưu DocumentEntity nếu có thay đổi
        documentRepository.save(document);

        // Nếu có file mới, upload và cập nhật danh sách file
        if (requestDto.getFiles() != null && !requestDto.getFiles().isEmpty()) {
            // Xóa file cũ (nếu cần)
            uploadService.deleteFiles(digitalDocument.getUploads());

            // Upload file mới
            Set<UploadEntity> uploadEntities = uploadService.uploadFiles(requestDto.getFiles());
            for (UploadEntity uploadEntity : uploadEntities) {
                uploadEntity.setDigitalDocument(digitalDocument);
            }
            digitalDocument.setUploads(uploadEntities);
        }

        // Lưu DigitalDocumentEntity
        digitalDocument = digitalDocumentRepository.save(digitalDocument);

        documentTextExtractionService.summarizeDoc(document.getDocumentId(),false);

        return digitalDocumentMapper.toResponseDto(digitalDocument);
    }

    @Transactional
    @Override
    public void deleteDigitalDocument(Long digitalDocumentId) {
        DigitalDocumentEntity digitalDocument = digitalDocumentRepository.findById(digitalDocumentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND,"Digital document not found"));

        digitalDocumentRepository.delete(digitalDocument); // Xóa tài liệu điện tử
    }
    @Transactional
    @Override
    public void updateVisibilityStatus(Long digitalDocumentId, VisibilityStatus status) {
        DigitalDocumentEntity digitalDocument = digitalDocumentRepository.findById(digitalDocumentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND,"Không tìm thấy tài liệu"));

        if (status == VisibilityStatus.PUBLIC && !documentApproved(digitalDocument)) {
            throw new AppException(ErrorCode.INSUFFICIENT_PERMISSIONS,"Tài liệu chưa được xét duyệt, không thể công khai");
        }

        digitalDocument.setVisibilityStatus(status);
        digitalDocumentRepository.save(digitalDocument);
    }

    public boolean hasPermissionToAccess(Long digitalDocumentId, UserEntity currentUser) {
        DigitalDocumentEntity documentEntity = digitalDocumentRepository.findById(digitalDocumentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));

        VisibilityStatus visibilityStatus = documentEntity.getVisibilityStatus();

        // Nếu tài liệu có trạng thái PUBLIC hoặc thuộc về người dùng hiện tại (PRIVATE)
        if (visibilityStatus == VisibilityStatus.PUBLIC || documentEntity.getUser().equals(currentUser)) {
            return true;
        }

        // Nếu tài liệu có trạng thái VIEW_ONLY, chỉ cho phép xem
        if (visibilityStatus == VisibilityStatus.RESTRICTED_VIEW) {
            // Nếu người dùng không phải là chủ sở hữu tài liệu, chỉ cho phép xem
            return currentUser.equals(documentEntity.getUser());
        }

        return false;
    }

    private boolean documentApproved(DigitalDocumentEntity digi) {
        // Giả định: Nếu có ít nhất một tệp tin đã được tải lên, tài liệu được xét duyệt
        DocumentEntity document = digi.getDocument();
        return document.getApprovalStatus().equals(ApprovalStatus.APPROVED);
    }
    @Transactional
    @Override
    public void approveDigitalDocument(Long digitalDocumentId) {
        DigitalDocumentEntity digitalDocument = digitalDocumentRepository.findById(digitalDocumentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND, "Không tìm thấy tài liệu"));

        // Cập nhật trạng thái tài liệu thành APPROVED (Đã duyệt)
        DocumentEntity document = digitalDocument.getDocument();
        document.setApprovalStatus(ApprovalStatus.APPROVED);
        documentRepository.save(document);

        documentTextExtractionService.summarizeDoc(document.getDocumentId(),true);
    }

    @Override
    public void rejectDigitalDocument(Long digitalDocumentId,String message) {
        DigitalDocumentEntity digitalDocument = digitalDocumentRepository.findById(digitalDocumentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND, "Không tìm thấy tài liệu"));

        // Cập nhật trạng thái tài liệu thành APPROVED (Đã duyệt)
        DocumentEntity document = digitalDocument.getDocument();
        document.setApprovalStatus(ApprovalStatus.REJECTED);
        if(message!=null && !message.isEmpty())
            document.setReason_approval(message);
        documentRepository.save(document);
    }

    @Transactional
    @Override
    public Page<DigitalDocumentResponseDto> getDigitalDocumentsByUser(Pageable pageable, String userId, DocumentFilterRequest filter) {
        // Đảm bảo sort theo uploadTime DESC nếu chưa có
        Pageable sortedPageable = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "uploadTime"));

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        checkAccessPermission(userId); // tách riêng quyền truy cập

        Specification<DigitalDocumentEntity> specification = buildSpecification(user, filter);

        return digitalDocumentRepository.findAll(specification, sortedPageable)
                .map(digitalDocumentMapper::toResponseDto);
    }


    @Override
    public Page<DigitalDocumentResponseDto> getDigitalAccessForUser(Pageable pageable) {
        UserEntity currentUser = authService.getCurrentUser();

        // B1: Lấy danh sách yêu cầu và sắp xếp theo requestTime giảm dần
        Page<DigitalDocumentEntity> docs = digitalDocumentRepository.findAccessibleDocumentsByUser(currentUser.getUserId(),pageable);

        return docs.map(digitalDocumentMapper::toResponseDto);
    }

    @Override
    public Page<DigitalDocumentResponseDto> getPendingApprovalDocuments(Pageable pageable) {
        List<ApprovalStatus> statuses = List.of(ApprovalStatus.PENDING, ApprovalStatus.REJECTED_BY_AI,ApprovalStatus.APPROVED_BY_AI);
        Page<DigitalDocumentEntity> documents = digitalDocumentRepository.findByApprovalStatuses(statuses, pageable);

        return documents.map(digitalDocumentMapper::toResponseDto);

    }

    private void checkAccessPermission(String userId) {
        UserEntity currentUser = authService.getCurrentUser();
        if (!roleService.isAdminOrManager(currentUser) && !currentUser.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }

    private Specification<DigitalDocumentEntity> buildSpecification(UserEntity user, DocumentFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("user"), user));

            // Các filter chuỗi
            if (isNotBlank(filter.getDocumentName())) {
                predicates.add(cb.like(cb.lower(root.get("document").get("documentName")), "%" + filter.getDocumentName().toLowerCase().trim() + "%"));
            }

            if (isNotBlank(filter.getAuthor())) {
                predicates.add(cb.like(cb.lower(root.get("document").get("author")), "%" + filter.getAuthor().toLowerCase().trim() + "%"));
            }

            if (isNotBlank(filter.getPublisher())) {
                predicates.add(cb.like(cb.lower(root.get("document").get("publisher")), "%" + filter.getPublisher().toLowerCase().trim() + "%"));
            }

            if (isNotBlank(filter.getLanguage())) {
                predicates.add(cb.equal(root.get("document").get("language"), filter.getLanguage().trim()));
            }

            if (filter.getCourseIds() != null && !filter.getCourseIds().isEmpty()) {
                Join<Object, Object> courseJoin = root.join("document").join("courses");
                predicates.add(courseJoin.get("id").in(filter.getCourseIds()));
            }

            if (filter.getDocumentTypeIds() != null && !filter.getDocumentTypeIds().isEmpty()) {
                Join<Object, Object> typeJoin = root.join("document").join("documentTypes");
                predicates.add(typeJoin.get("id").in(filter.getDocumentTypeIds()));
            }

            // Filter theo enum
            if (filter.getDocumentCategory() != null) {
                predicates.add(cb.equal(root.get("document").get("documentCategory"), filter.getDocumentCategory()));
            }

            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("document").get("status"), filter.getStatus()));
            }

            if (filter.getApprovalStatus() != null) {
                predicates.add(cb.equal(root.get("document").get("approvalStatus"), filter.getApprovalStatus()));
            }

            // Filter ngày
            if (filter.getPublishedDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("document").get("publishedDate"), filter.getPublishedDateFrom()));
            }

            if (filter.getPublishedDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("document").get("publishedDate"), filter.getPublishedDateTo()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }


    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }


}
