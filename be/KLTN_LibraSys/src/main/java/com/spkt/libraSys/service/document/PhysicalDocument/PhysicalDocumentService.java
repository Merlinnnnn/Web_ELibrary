package com.spkt.libraSys.service.document.PhysicalDocument;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for managing physical documents in the library system.
 * Provides operations for creating, retrieving, updating, and deleting physical documents,
 * as well as handling document reservations.
 */
public interface PhysicalDocumentService {
    /**
     * Creates a new physical document in the system.
     * @param requestDto The physical document creation request data
     * @return The created physical document response
     */
    PhysicalResponseDto createPhysicalDocument(PhysicalRequestDto requestDto);

    /**
     * Retrieves a physical document by its ID.
     * @param id The ID of the physical document to retrieve
     * @return The physical document response
     */
    PhysicalResponseDto getPhysicalDocumentById(Long id);

    /**
     * Retrieves all physical documents with pagination.
     * @param pageable Pagination information
     * @return A page of physical document responses
     */
    Page<PhysicalResponseDto> getAllPhysicalDocuments(Pageable pageable);

    /**
     * Updates an existing physical document.
     * @param id The ID of the physical document to update
     * @param requestDto The updated physical document data
     * @return The updated physical document response
     */
    PhysicalResponseDto updatePhysicalDocument(Long id, PhysicalRequestDto requestDto);

    /**
     * Deletes a physical document from the system.
     * @param id The ID of the physical document to delete
     */
    void deletePhysicalDocument(Long id);

    /**
     * Reserves a physical document for a user.
     * @param id The ID of the physical document to reserve
     */
    void reservePhysicalDocument(Long id);
}
