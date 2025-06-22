package com.spkt.libraSys.service.document.DocumentType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interface providing methods for managing document types in the library system.
 */
public interface DocumentTypeService {

    /**
     * Retrieve a document type by its ID.
     *
     * @param documentTypeId ID of the document type to retrieve
     * @return DocumentTypeResponse containing the document type information
     */
    DocumentTypeResponse getIdDocumentType(Long documentTypeId);

    /**
     * Retrieve all document types with pagination.
     *
     * @param pageable Pagination information
     * @return Page containing document types
     */
    Page<DocumentTypeResponse> getAllDocumentTypes(Pageable pageable);

    /**
     * Create a new document type.
     *
     * @param request DTO containing the document type information to create
     * @return DTO containing the created document type information
     */
    DocumentTypeResponse createDocumentType(DocumentTypeRequestDto request);

    /**
     * Update an existing document type.
     *
     * @param id ID of the document type to update
     * @param request DTO containing new information to update the document type
     * @return DTO containing the updated document type information
     */
    DocumentTypeResponse updateDocumentType(Long id, DocumentTypeRequestDto request);

    /**
     * Delete a document type by ID.
     *
     * @param id ID of the document type to delete
     */
    void deleteDocumentType(Long id);
}
