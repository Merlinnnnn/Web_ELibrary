package com.spkt.libraSys.service.document.DigitalDocument;

import com.spkt.libraSys.service.PageDTO;
import com.spkt.libraSys.service.document.DigitalDocument.DigitalDocumentResponseDto;
import com.spkt.libraSys.service.document.DigitalDocument.DigitalDocumentRequestDto;
import com.spkt.libraSys.service.document.DocumentFilterRequest;
import com.spkt.libraSys.service.user.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for managing digital documents in the library system.
 * Provides functionality for creating, reading, updating, and deleting digital documents,
 * as well as managing document visibility, access permissions, and approval workflows.
 */
public interface DigitalDocumentService {

    /**
     * Creates a new digital document with the provided information and files.
     * @param requestDto The document creation request containing document details and files
     * @return DigitalDocumentResponseDto containing the created document information
     * @throws Exception if there's an error during file upload or document creation
     */
    DigitalDocumentResponseDto createDigitalDocument(DigitalDocumentRequestDto requestDto) throws Exception;

    /**
     * Retrieves a digital document by its ID.
     * @param digitalDocumentId The ID of the document to retrieve
     * @return DigitalDocumentResponseDto containing the document information
     */
    DigitalDocumentResponseDto getDigitalDocument(Long digitalDocumentId);

    /**
     * Retrieves all digital documents with pagination support.
     * @param pageable Pagination and sorting information
     * @return Page of DigitalDocumentResponseDto containing the documents
     */
    Page<DigitalDocumentResponseDto> getAllDigitalDocuments(Pageable pageable);

    /**
     * Updates an existing digital document with new information and files.
     * @param digitalDocumentId The ID of the document to update
     * @param requestDto The updated document information and files
     * @return DigitalDocumentResponseDto containing the updated document information
     * @throws Exception if there's an error during file upload or document update
     */
    DigitalDocumentResponseDto updateDigitalDocument(Long digitalDocumentId, DigitalDocumentRequestDto requestDto) throws Exception;

    /**
     * Deletes a digital document by its ID.
     * @param digitalDocumentId The ID of the document to delete
     */
    void deleteDigitalDocument(Long digitalDocumentId);

    /**
     * Updates the visibility status of a digital document.
     * @param digitalDocumentId The ID of the document
     * @param status The new visibility status
     */
    void updateVisibilityStatus(Long digitalDocumentId, VisibilityStatus status);

    /**
     * Checks if a user has permission to access a specific digital document.
     * @param digitalDocumentId The ID of the document
     * @param currentUser The user to check permissions for
     * @return true if the user has access, false otherwise
     */
    boolean hasPermissionToAccess(Long digitalDocumentId, UserEntity currentUser);

    /**
     * Approves a digital document for publication.
     * @param digitalDocumentId The ID of the document to approve
     */
    void approveDigitalDocument(Long digitalDocumentId);

    /**
     * Rejects a digital document with an optional rejection message.
     * @param digitalDocumentId The ID of the document to reject
     * @param message Optional message explaining the rejection reason
     */
    void rejectDigitalDocument(Long digitalDocumentId, String message);

    /**
     * Retrieves digital documents uploaded by a specific user with filtering options.
     * @param pageable Pagination and sorting information
     * @param userId The ID of the user
     * @param filter Filter criteria for the documents
     * @return Page of DigitalDocumentResponseDto containing the filtered documents
     */
    Page<DigitalDocumentResponseDto> getDigitalDocumentsByUser(Pageable pageable, String userId, DocumentFilterRequest filter);

    /**
     * Retrieves all digital documents that a user has access to.
     * @param pageable Pagination and sorting information
     * @return Page of DigitalDocumentResponseDto containing accessible documents
     */
    Page<DigitalDocumentResponseDto> getDigitalAccessForUser(Pageable pageable);

    /**
     * Retrieves all documents that are pending approval.
     * @param pageable Pagination and sorting information
     * @return Page of DigitalDocumentResponseDto containing pending documents
     */
    Page<DigitalDocumentResponseDto> getPendingApprovalDocuments(Pageable pageable);
}
