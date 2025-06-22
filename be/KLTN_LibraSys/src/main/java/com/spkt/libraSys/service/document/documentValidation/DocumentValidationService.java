package com.spkt.libraSys.service.document.documentValidation;

import com.spkt.libraSys.service.document.DocumentCreateRequest;
import com.spkt.libraSys.service.document.DocumentEntity;
import com.spkt.libraSys.service.document.DocumentUploadRequestDto;
import com.spkt.libraSys.service.document.PhysicalDocument.PhysicalRequestDto;

/**
 * Service interface for validating different types of document requests.
 * Provides methods to validate physical documents, document uploads, and document creation requests.
 */
public interface DocumentValidationService {
    /**
     * Validates a physical document request.
     * Checks if the physical document request meets all required criteria and constraints.
     *
     * @param request The physical document request to validate
     */
    void validateDocument(PhysicalRequestDto request);

    /**
     * Validates a document upload request.
     * Ensures the uploaded document meets all required criteria and constraints.
     *
     * @param request The document upload request to validate
     */
    void validateDocument(DocumentUploadRequestDto request);

    /**
     * Validates a document creation request.
     * Verifies if the document creation request contains all necessary information and meets validation rules.
     *
     * @param request The document creation request to validate
     */
    void validateDocument(DocumentCreateRequest request);

    /**
     * Retrieves a document entity by its ID.
     * Used for validation purposes to check document existence and properties.
     *
     * @param documentId The ID of the document to retrieve
     * @return DocumentEntity containing the document information
     */
    DocumentEntity getDocumentById(Long documentId);
}
