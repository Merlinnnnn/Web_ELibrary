package com.spkt.libraSys.service.document.DigitalDocument;

import com.spkt.libraSys.exception.ApiResponse;
import com.spkt.libraSys.service.PageDTO;
import com.spkt.libraSys.service.document.DigitalDocument.AccessRequest.AccessRequestResponseDto;
import com.spkt.libraSys.service.document.DigitalDocument.AccessRequest.AccessRequestService;
import com.spkt.libraSys.service.document.DigitalDocument.DigitalDocumentRequestDto;
import com.spkt.libraSys.service.document.DigitalDocument.DigitalDocumentResponseDto;
import com.spkt.libraSys.service.document.DigitalDocument.DigitalDocumentService;
import com.spkt.libraSys.service.document.DocumentCategory;
import com.spkt.libraSys.service.document.DocumentFilterRequest;
import com.spkt.libraSys.service.document.DocumentStatus;
import com.spkt.libraSys.service.document.upload.FileStreamResponse;
import com.spkt.libraSys.service.document.upload.UploadService;
import com.spkt.libraSys.service.document.viewer.ApprovalStatus;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/digital-documents")
public class DigitalDocumentController {

    @Autowired
    private DigitalDocumentService digitalDocumentService;
    @Autowired
    private AccessRequestService accessRequestService;
    @Autowired
    private UploadService uploadService;

    /**
     * **Create digital document**
     * @param request Document creation request DTO
     * @return `ApiResponse<DigitalDocumentResponseDto>`
     * @throws IOException if there's an error uploading the file
     */
    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<ApiResponse<DigitalDocumentResponseDto>> createDigitalDocument(
            @ModelAttribute @Valid DigitalDocumentRequestDto request) throws Exception {

        // Call service to create digital document
        DigitalDocumentResponseDto response = digitalDocumentService.createDigitalDocument(request);

        // Build ApiResponse to return
        ApiResponse<DigitalDocumentResponseDto> apiResponse = ApiResponse.<DigitalDocumentResponseDto>builder()
                .message("Tài liệu đã được tạo thành công")
                .data(response)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    /**
     * **Get digital document information by ID**
     * @param id Document ID
     * @return `ApiResponse<DigitalDocumentResponseDto>`
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DigitalDocumentResponseDto>> getDigitalDocumentById(@PathVariable Long id) {
        // Call service to get digital document information
        DigitalDocumentResponseDto response = digitalDocumentService.getDigitalDocument(id);

        // Build ApiResponse to return
        ApiResponse<DigitalDocumentResponseDto> apiResponse = ApiResponse.<DigitalDocumentResponseDto>builder()
                .message("Lấy tài liệu thành công")
                .data(response)
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/user/access")
    public ResponseEntity<ApiResponse<PageDTO<DigitalDocumentResponseDto>>> getAccessRequestsByUser(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "digital_document_id,asc") String sort) throws Exception {

        String[] sortParams = sort.split(",");
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.by(sortParams[0]).with(Sort.Direction.fromString(sortParams[1]))));

        Page<DigitalDocumentResponseDto> pageResponse = digitalDocumentService.getDigitalAccessForUser(pageable);

        ApiResponse< PageDTO<DigitalDocumentResponseDto>> apiResponse = ApiResponse.<PageDTO<DigitalDocumentResponseDto>>builder()
                .message("Danh sách tai lieu co the truy cập của người dùng")
                .data(new PageDTO<>(pageResponse))
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    /**
     * **Update digital document**
     * @param id ID of document to update
     * @param request Document update request DTO
     * @return `ApiResponse<DigitalDocumentResponseDto>`
     * @throws IOException if there's an error uploading the file
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DigitalDocumentResponseDto>> updateDigitalDocument(
            @PathVariable Long id, @ModelAttribute @Valid DigitalDocumentRequestDto request) throws Exception {

        // Call service to update digital document
        DigitalDocumentResponseDto response = digitalDocumentService.updateDigitalDocument(id, request);

        // Build ApiResponse to return
        ApiResponse<DigitalDocumentResponseDto> apiResponse = ApiResponse.<DigitalDocumentResponseDto>builder()
                .message("Tài liệu đã được cập nhật thành công")
                .data(response)
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageDTO<DigitalDocumentResponseDto>>> getAllDigitalDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "document.documentName") String sort,
            @RequestParam(defaultValue = "asc") String direction) {

        Sort sortBy;
        try {
            Sort.Direction sortDirection = Sort.Direction.fromString(direction);
            sortBy = Sort.by(sortDirection, sort);
        } catch (IllegalArgumentException e) {
            ApiResponse<PageDTO<DigitalDocumentResponseDto>> apiResponse = ApiResponse.<PageDTO<DigitalDocumentResponseDto>>builder()
                    .message("Hướng sắp xếp không hợp lệ. Sử dụng 'asc' hoặc 'desc'.")
                    .data(null)
                    .build();
            return ResponseEntity.ok(apiResponse);
        }

        Pageable pageable = PageRequest.of(page, size, sortBy);
        Page<DigitalDocumentResponseDto> responseList = digitalDocumentService.getAllDigitalDocuments(pageable);
        PageDTO<DigitalDocumentResponseDto> pageDTO = new PageDTO<>(responseList);

        ApiResponse<PageDTO<DigitalDocumentResponseDto>> apiResponse = ApiResponse.<PageDTO<DigitalDocumentResponseDto>>builder()
                .message("Danh sách tài liệu số đã được lấy thành công.")
                .data(pageDTO)
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    /**
     * **Delete digital document by ID**
     * @param id ID of document to delete
     * @return `ApiResponse<Void>`
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<ApiResponse<Void>> deleteDigitalDocument(@PathVariable Long id) {
        // Call service to delete digital document
        digitalDocumentService.deleteDigitalDocument(id);

        // Build ApiResponse to return
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .message("Tài liệu đã được xóa thành công")
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/{id}/visibility")
    public ResponseEntity<ApiResponse<String>> updateVisibilityStatus(
            @PathVariable("id") Long digitalDocumentId,
            @RequestParam("status") VisibilityStatus status) {

        digitalDocumentService.updateVisibilityStatus(digitalDocumentId, status);

        ApiResponse<String> response = ApiResponse.<String>builder()
                .message("Tài liệu ID " + digitalDocumentId + " đã được cập nhật thành " + status)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/approve/{digitalDocumentId}")
    public ResponseEntity<ApiResponse<String>> approveDigitalDocument(@PathVariable Long digitalDocumentId) {
        // Only allow administrators to approve documents
        digitalDocumentService.approveDigitalDocument(digitalDocumentId);

        return ResponseEntity.ok(ApiResponse.<String>builder()
                .message("Tài liệu đã được phê duyệt")
                .build());
    }

    @PostMapping("/recject/{digitalDocumentId}")
    public ResponseEntity<ApiResponse<String>> rejectDigitalDocument(@PathVariable Long digitalDocumentId,
                                                                     @RequestParam(required = false) String message) {
        // Only allow administrators to approve documents
        digitalDocumentService.rejectDigitalDocument(digitalDocumentId,message);

        return ResponseEntity.ok(ApiResponse.<String>builder()
                .message("Tài liệu khong được phê duyệt")
                .build());
    }

    @GetMapping("/pending-approval")
    public ResponseEntity<ApiResponse<PageDTO<DigitalDocumentResponseDto>>> getDocumentsPendingApproval(
            @PageableDefault(size = 10, sort = "digitalDocumentId", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<DigitalDocumentResponseDto> pageResult = digitalDocumentService.getPendingApprovalDocuments(pageable);

        return ResponseEntity.ok(ApiResponse.<PageDTO<DigitalDocumentResponseDto>>builder()
                .data(new PageDTO<>(pageResult))
                .message("Danh sách tài liệu chờ phê duyệt (phân trang)")
                .build());
    }

    // Documents uploaded by user
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<PageDTO<DigitalDocumentResponseDto>>> getDigitalDocumentsByUser(
            @PathVariable String userId,
            @RequestParam(required = false) String documentName,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String publisher,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) Set<Long> courseIds,
            @RequestParam(required = false) Set<Long> documentTypeIds,
            @RequestParam(required = false) DocumentCategory documentCategory,
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(required = false) ApprovalStatus approvalStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate publishedDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate publishedDateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "uploadTime"));

        DocumentFilterRequest filter = new DocumentFilterRequest();
        filter.setDocumentName(documentName);
        filter.setAuthor(author);
        filter.setPublisher(publisher);
        filter.setLanguage(language);
        filter.setCourseIds(courseIds);
        filter.setDocumentTypeIds(documentTypeIds);
        filter.setDocumentCategory(documentCategory);
        filter.setStatus(status);
        filter.setApprovalStatus(approvalStatus);
        filter.setPublishedDateFrom(publishedDateFrom);
        filter.setPublishedDateTo(publishedDateTo);

        Page<DigitalDocumentResponseDto> documents = digitalDocumentService.getDigitalDocumentsByUser(pageable, userId, filter);
        PageDTO<DigitalDocumentResponseDto> pageDTO = new PageDTO<>(documents);

        ApiResponse<PageDTO<DigitalDocumentResponseDto>> apiResponse = ApiResponse.<PageDTO<DigitalDocumentResponseDto>>builder()
                .message("Danh sách tài liệu của người dùng.")
                .data(pageDTO)
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @GetMapping("/file/{uploadId}")
    public ResponseEntity<StreamingResponseBody> getFile(@PathVariable Long uploadId) throws Exception {
        // Lấy file dưới dạng stream và kiểu MIME
        FileStreamResponse fileStreamResponse = uploadService.getFileAsStream(uploadId);

        // Lấy file stream và fileType từ đối tượng FileStreamResponse
        Resource file = fileStreamResponse.getFile();
        String fileType = fileStreamResponse.getFileType();

        // Lấy tên file
        String fileName = file.getFilename();

        // Sử dụng StreamingResponseBody để trả về file dưới dạng stream mà không tải toàn bộ vào bộ nhớ
        StreamingResponseBody stream = out -> {
            try (InputStream inputStream = file.getInputStream()) {
                byte[] buffer = new byte[1024];  // Đọc file theo từng khối (chunk)
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    out.write(buffer, 0, length);
                }
            }
        };

        // Trả về ResponseEntity với file stream và fileType
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileType))  // Chỉ định kiểu MIME
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")  // Tải về file
                .body(stream);
    }
}
