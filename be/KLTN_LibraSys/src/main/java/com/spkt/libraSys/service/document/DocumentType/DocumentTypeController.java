package com.spkt.libraSys.service.document.DocumentType;

import com.spkt.libraSys.exception.ApiResponse;
import com.spkt.libraSys.service.PageDTO;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller class for managing document type requests.
 * Provides endpoints for creating, updating, retrieving, and deleting document types.
 */
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/v1/document-types")
public class DocumentTypeController {

    DocumentTypeService documentTypeService;

    /**
     * Retrieve document type by ID.
     *
     * @param id ID of the document type to retrieve.
     * @return ResponseEntity containing the retrieved document type.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentTypeResponse>> getIdDocumentTypes(@PathVariable Long id) {
        DocumentTypeResponse documentType = documentTypeService.getIdDocumentType(id);
        ApiResponse<DocumentTypeResponse> apiResponse = ApiResponse.<DocumentTypeResponse>builder()
                .message("Loại tài liệu đã được truy xuất thành công")
                .data(documentType)
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Retrieve all document types with pagination.
     *
     * @param pageable Pagination information (page number, page size, sort direction).
     * @return ResponseEntity containing the paginated list of document types.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageDTO<DocumentTypeResponse>>> getAllDocumentTypes(Pageable pageable) {
        Page<DocumentTypeResponse> documentTypes = documentTypeService.getAllDocumentTypes(pageable);
        PageDTO<DocumentTypeResponse> pageDTO = new PageDTO<>(documentTypes);
        ApiResponse<PageDTO<DocumentTypeResponse>> apiResponse = ApiResponse.<PageDTO<DocumentTypeResponse>>builder()
                .message("Các loại tài liệu đã được truy xuất thành công")
                .data(pageDTO)
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Create a new document type.
     *
     * @param request Document type creation request, containing necessary information to create a new document type.
     * @return ResponseEntity containing the newly created document type.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<DocumentTypeResponse>> createDocumentType(
            @Valid @RequestBody DocumentTypeRequestDto request) {
        DocumentTypeResponse documentType = documentTypeService.createDocumentType(request);
        ApiResponse<DocumentTypeResponse> apiResponse = ApiResponse.<DocumentTypeResponse>builder()
                .message("Loại tài liệu đã được tạo thành công")
                .data(documentType)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    /**
     * Update an existing document type.
     *
     * @param documentTypeId ID of the document type to update.
     * @param request Update request containing new information to modify the document type.
     * @return ResponseEntity containing the updated document type.
     */
    @PutMapping("/{documentTypeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DocumentTypeResponse>> updateDocumentType(
            @PathVariable Long documentTypeId,
            @Valid @RequestBody DocumentTypeRequestDto request) {
        DocumentTypeResponse documentType = documentTypeService.updateDocumentType(documentTypeId, request);
        ApiResponse<DocumentTypeResponse> apiResponse = ApiResponse.<DocumentTypeResponse>builder()
                .message("Loại tài liệu đã được cập nhật thành công")
                .data(documentType)
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Delete a document type from the system.
     *
     * @param documentTypeId ID of the document type to delete.
     * @return ResponseEntity indicating successful deletion of the document type.
     */
    @DeleteMapping("/{documentTypeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteDocumentType(@PathVariable Long documentTypeId) {
        documentTypeService.deleteDocumentType(documentTypeId);
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .message("Loại tài liệu đã được xóa thành công")
                .build();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(apiResponse);
    }
}
