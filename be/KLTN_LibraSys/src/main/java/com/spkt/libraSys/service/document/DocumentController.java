package com.spkt.libraSys.service.document;

import com.spkt.libraSys.exception.ApiResponse;
import com.spkt.libraSys.service.PageDTO;
import com.spkt.libraSys.service.document.favorite.FavoriteDocumentService;
import com.spkt.libraSys.service.document.viewer.ApprovalStatus;
import com.spkt.libraSys.service.helper.HelperService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Controller class that manages API requests related to documents.
 * Provides endpoints for creating, updating, searching, and managing documents.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/v1/documents")
public class DocumentController {

    DocumentService documentService;
    FavoriteDocumentService favoriteDocumentService;
    private final HelperService helperService;

    /**
     * Creates a new document.
     * This endpoint allows users to create a new document.
     *
     * @param request Document creation request, including document details.
     * @return ApiResponse containing the created document information.
     */
    @Operation(summary = "Create new document",
            description = "This endpoint allows users to create a new document.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",
                            description = "Document created successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ApiResponse.class))),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request",
                            content = @Content(mediaType = "application/json"))
            })
    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<DocumentResponseDto>> createDocument(
            @ModelAttribute @Valid DocumentCreateRequest request) throws IOException {
        DocumentResponseDto response = documentService.createDocument(request);
        ApiResponse<DocumentResponseDto> apiResponse = ApiResponse.<DocumentResponseDto>builder()
                .message("Tài liệu đã được tạo thành công")
                .data(response)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    /**
     * Retrieves document information by ID.
     * @param id ID of the document to retrieve
     * @return ApiResponse containing the document information
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentResponseDto>> getDocumentById(@PathVariable Long id) {
        DocumentResponseDto response = documentService.getDocumentById(id);

        ApiResponse<DocumentResponseDto> apiResponse = ApiResponse.<DocumentResponseDto>builder()
                .message("Lấy tài liệu thành công")
                .data(response)
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity< ApiResponse<Void> >deleteDocument(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Tài liệu đã được xóa thành công")
                .build());
    }

    /**
     * Retrieves all documents with pagination and sorting.
     * This endpoint allows retrieving all documents with pagination and sorting options.
     *
     * @param page Page number for pagination
     * @param size Size of each page
     * @param sort Field to sort documents by
     * @param direction Sort direction ("asc" or "desc")
     * @return ApiResponse containing the paginated list of documents
     */
    @GetMapping
    public ResponseEntity<  ApiResponse<PageDTO<DocumentResponseDto>>> getAllDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "documentName") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        Sort sortBy;
        try {
            Sort.Direction sortDirection = Sort.Direction.fromString(direction);
            sortBy = Sort.by(sortDirection, sort);
        } catch (IllegalArgumentException e) {
            ApiResponse<PageDTO<DocumentResponseDto>> apiResponse =  ApiResponse.<PageDTO<DocumentResponseDto>>builder()
                    .message("Hướng sắp xếp không hợp lệ. Sử dụng 'asc' hoặc 'desc'.")
                    .data(null)
                    .build();
            return ResponseEntity.ok(apiResponse);
        }
        Pageable pageable = PageRequest.of(page, size, sortBy);
        Page<DocumentResponseDto> responseList = documentService.getAllDocuments(pageable);
        PageDTO<DocumentResponseDto> pageDTO = new PageDTO<>(responseList);
        ApiResponse<PageDTO<DocumentResponseDto>> apiResponse =  ApiResponse.<PageDTO<DocumentResponseDto>>builder()
                .message("Tài liệu đã được lấy thành công")
                .data(pageDTO)
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Marks a document as favorite.
     *
     * @param id ID of the document to mark as favorite
     * @return ResponseEntity<ApiResponse> containing the operation result
     */
    @PostMapping("/{id}/favorite")
    public ResponseEntity<ApiResponse<Void>> favoriteDocument(@PathVariable Long id) {
        favoriteDocumentService.markFavorite(id);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .message("Tài liệu đã được đánh dấu yêu thích thành công")
                .data(null)
                .build();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/favorite")
    public ResponseEntity<ApiResponse<Void>> unfavorite(@PathVariable Long id) {
        favoriteDocumentService.unmarkFavorite(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .message("Tài liệu đã được gỡ khỏi danh sách yêu thích")
                        .data(null)
                        .build()
        );
    }

    @GetMapping("/{id}/is-favorite")
    public ResponseEntity<ApiResponse<Boolean>> isFavoriteDocument(@PathVariable Long id) {
        boolean isFavorite = favoriteDocumentService.isFavorite(id);
        return ResponseEntity.ok(
                ApiResponse.<Boolean>builder()
                .message("Trạng thái yêu thích đã được lấy thành công")
                .data(isFavorite)
                .build());
    }

    /**
     * Deletes multiple documents by their IDs.
     * This endpoint allows deleting multiple documents at once by providing a list of document IDs.
     *
     * @param documentIds List of document IDs to delete
     * @return ApiResponse containing the deletion result
     */
    @DeleteMapping("/batch")
    public ApiResponse<Void> deleteDocumentsByIds(@RequestBody List<Long> documentIds) {
        documentService.deleteDocumentsByIds(documentIds);
        return ApiResponse.<Void>builder()
                .message("Tài liệu đã được đánh dấu là không khả dụng thành công")
                .build();
    }

    @GetMapping("/filter")
    public ResponseEntity<Page<DocumentResponseDto>> filterDocuments(
            @RequestParam(required = false) String documentName,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String publisher,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) Set<Long> courseIds,
            @RequestParam(required = false) Set<Long> documentTypeIds,
            @RequestParam(required = false) DocumentCategory documentCategory,
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(required = false) ApprovalStatus approvalStatus,
            @RequestParam(required = false) String publishedDateFrom,
            @RequestParam(required = false) String publishedDateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "documentId") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        DocumentFilterRequest filterRequest = new DocumentFilterRequest();
        filterRequest.setDocumentName(documentName);
        filterRequest.setAuthor(author);
        filterRequest.setPublisher(publisher);
        filterRequest.setLanguage(language);
        filterRequest.setCourseIds(courseIds);
        filterRequest.setDocumentTypeIds(documentTypeIds);
        filterRequest.setDocumentCategory(documentCategory);
        filterRequest.setStatus(status);
        filterRequest.setApprovalStatus(approvalStatus);

        // Convert date strings to LocalDate if provided
        if (publishedDateFrom != null && !publishedDateFrom.isEmpty()) {
            filterRequest.setPublishedDateFrom(java.time.LocalDate.parse(publishedDateFrom));
        }
        if (publishedDateTo != null && !publishedDateTo.isEmpty()) {
            filterRequest.setPublishedDateTo(java.time.LocalDate.parse(publishedDateTo));
        }

        // Create pageable object with sorting
        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<DocumentResponseDto> result = documentService.filterDocuments(filterRequest, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageDTO<DocumentResponseDto>>> searchByTitle(
            @RequestParam String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "documentName") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort sortBy;
        try {
            Sort.Direction sortDirection = Sort.Direction.fromString(direction);
            sortBy = Sort.by(sortDirection, sort);
        } catch (IllegalArgumentException e) {
            ApiResponse<PageDTO<DocumentResponseDto>> apiResponse = ApiResponse.<PageDTO<DocumentResponseDto>>builder()
                    .message("Hướng sắp xếp không hợp lệ. Sử dụng 'asc' hoặc 'desc'.")
                    .data(null)
                    .build();
            return ResponseEntity.ok(apiResponse);
        }

        Pageable pageable = PageRequest.of(page, size, sortBy);
        Page<DocumentResponseDto> responseList = documentService.searchByTitle(title, pageable);
        PageDTO<DocumentResponseDto> pageDTO = new PageDTO<>(responseList);
        
        ApiResponse<PageDTO<DocumentResponseDto>> apiResponse = ApiResponse.<PageDTO<DocumentResponseDto>>builder()
                .message("Tìm kiếm tài liệu thành công")
                .data(pageDTO)
                .build();
                
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * API to get statistics about borrowed and favorite books for a user.
     *
     * @param userId User ID
     * @return Statistics information about the user's books
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics(@PathVariable String userId) {
        Map<String,Object> rep = helperService.getStaticDocForUser(userId);

        return  ResponseEntity.ok(
                ApiResponse.<Map<String, Object>>builder()
                        .message("Thông tin thống kê về sách của người dùng")
                        .data(rep)
                        .build()
        );
    }
}

