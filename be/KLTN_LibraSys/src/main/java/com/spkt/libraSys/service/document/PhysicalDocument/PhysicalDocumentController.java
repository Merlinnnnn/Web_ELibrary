package com.spkt.libraSys.service.document.PhysicalDocument;

import com.spkt.libraSys.exception.ApiResponse;
import com.spkt.libraSys.service.PageDTO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing physical documents in the library system.
 * Provides endpoints for CRUD operations on physical documents.
 */
@RestController
@RequestMapping("/api/v1/physical-documents")
public class PhysicalDocumentController {

    @Autowired
    private PhysicalDocumentService physicalDocumentService;

    /**
     * Creates a new physical document.
     * @param requestDto The physical document creation request data
     * @return ResponseEntity containing the created physical document
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PhysicalResponseDto>> createPhysicalDocument(
            @RequestBody @Valid PhysicalRequestDto requestDto) {
        PhysicalResponseDto response = physicalDocumentService.createPhysicalDocument(requestDto);
        ApiResponse<PhysicalResponseDto> apiResponse = ApiResponse.<PhysicalResponseDto>builder()
                .message("Tài liệu vật lý đã được tạo thành công")
                .data(response)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    /**
     * Retrieves a physical document by its ID.
     * @param id The ID of the physical document to retrieve
     * @return ResponseEntity containing the physical document
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PhysicalResponseDto>> getPhysicalDocument(@PathVariable Long id) {
        PhysicalResponseDto response = physicalDocumentService.getPhysicalDocumentById(id);
        ApiResponse<PhysicalResponseDto> apiResponse = ApiResponse.<PhysicalResponseDto>builder()
                .message("Lấy tài liệu vật lý thành công")
                .data(response)
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Updates an existing physical document.
     * @param id The ID of the physical document to update
     * @param requestDto The updated physical document data
     * @return ResponseEntity containing the updated physical document
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PhysicalResponseDto>> updatePhysicalDocument(
            @PathVariable Long id, @RequestBody @Valid PhysicalRequestDto requestDto) {
        PhysicalResponseDto response = physicalDocumentService.updatePhysicalDocument(id, requestDto);
        ApiResponse<PhysicalResponseDto> apiResponse = ApiResponse.<PhysicalResponseDto>builder()
                .message("Tài liệu vật lý đã được cập nhật thành công")
                .data(response)
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Retrieves all physical documents with pagination and sorting.
     * @param page Page number (default: 0)
     * @param size Page size (default: 10)
     * @param sort Sort field (default: document.documentName)
     * @param direction Sort direction (default: asc)
     * @return ResponseEntity containing a page of physical documents
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageDTO<PhysicalResponseDto>>> getAllPhysicalDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "document.documentName") String sort,
            @RequestParam(defaultValue = "asc") String direction) {

        Sort sortBy;
        try {
            Sort.Direction sortDirection = Sort.Direction.fromString(direction);
            sortBy = Sort.by(sortDirection, sort);
        } catch (IllegalArgumentException e) {
            ApiResponse<PageDTO<PhysicalResponseDto>> apiResponse = ApiResponse.<PageDTO<PhysicalResponseDto>>builder()
                    .message("Hướng sắp xếp không hợp lệ. Sử dụng 'asc' hoặc 'desc'.")
                    .data(null)
                    .build();
            return ResponseEntity.ok(apiResponse);
        }

        Pageable pageable = PageRequest.of(page, size, sortBy);
        Page<PhysicalResponseDto> responseList = physicalDocumentService.getAllPhysicalDocuments(pageable);
        PageDTO<PhysicalResponseDto> pageDTO = new PageDTO<>(responseList);

        ApiResponse<PageDTO<PhysicalResponseDto>> apiResponse = ApiResponse.<PageDTO<PhysicalResponseDto>>builder()
                .message("Danh sách tài liệu vật lý đã được lấy thành công.")
                .data(pageDTO)
                .build();
                
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Deletes a physical document.
     * @param id The ID of the physical document to delete
     * @return ResponseEntity with no content
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<ApiResponse<Void>> deletePhysicalDocument(@PathVariable Long id) {
        physicalDocumentService.deletePhysicalDocument(id);
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .message("Tài liệu vật lý đã được xóa thành công")
                .build();
        return ResponseEntity.ok(apiResponse);
    }
}
