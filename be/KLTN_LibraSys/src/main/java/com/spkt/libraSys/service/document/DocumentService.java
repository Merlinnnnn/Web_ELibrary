package com.spkt.libraSys.service.document;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.IOException;
import java.util.List;

/**
 * Interface providing methods for managing documents in the library system.
 */
public interface DocumentService {

    DocumentResponseDto createDocument(DocumentCreateRequest request) throws IOException;
//
    /**
     * Updates the information of a document by ID.
     *
     * @param id      ID of the document to update
     * @param request DTO containing new information to update the document
     * @return DTO containing the updated document information
     */
    DocumentResponseDto updateDocument(Long id, DocumentUpdateRequest request);

    /**
     * Retrieves detailed information of a document by ID.
     *
     * @param id ID of the document to retrieve
     * @return DTO containing the detailed document information
     */
    DocumentResponseDto getDocumentById(Long id);

//    /**
//     * Tìm kiếm tài liệu theo các tiêu chí đã cung cấp.
//     *
//     * @param searchRequest DTO chứa các tham số tìm kiếm (ví dụ: tên tài liệu, tác giả, loại tài liệu).
//     * @param pageable      Thông tin phân trang.
//     * @return Trang chứa danh sách tài liệu thỏa mãn điều kiện tìm kiếm.
//     */
//    Page<DocumentResponseDto> searchDocuments(DocumentSearchRequest searchRequest, Pageable pageable);

    /**
     * Retrieves all documents in the system with pagination.
     *
     * @param pageable Pagination information
     * @return Page containing all documents
     */
    Page<DocumentResponseDto> getAllDocuments(Pageable pageable);

    /**
     * Deletes a document by ID.
     *
     * @param id ID of the document to delete
     */
    @PreAuthorize("hasRole('ADMIN')")
    void deleteDocument(Long id);


    /**
     * Classifies a document into a new type.
     *
     * @param id           ID of the document to classify
     * @param newTypeName  New type name for the document
     */
    void classifyDocument(Long id, String newTypeName);



    /**
     * Deletes a list of documents by their IDs.
     *
     * @param documentIds List of document IDs to delete
     */
    @PreAuthorize("hasRole('ADMIN')")
    void deleteDocumentsByIds(List<Long> documentIds);

    /**
     * Retrieves the content of a document page as byte[].
     *
     * @param documentId ID of the document
     * @param pageNumber Page number to retrieve content from
     * @return Byte array containing the document page content
     */
    byte[] getDocumentPageContent(Long documentId, int pageNumber);

    /**
     * Searches for a document by title.
     *
     * @param title Title to search for
     * @return DTO containing the found document information, null if not found
     * @deprecated Use {@link #searchByTitle(String, Pageable)} instead
     */
    @Deprecated
    DocumentResponseDto searchByTitle(String title);

    /**
     * Searches for documents by title with pagination.
     *
     * @param title Title to search for (partial match)
     * @param pageable Pagination information
     * @return Page containing the list of documents matching the search criteria
     */
    Page<DocumentResponseDto> searchByTitle(String title, Pageable pageable);

    /**
     * Filters documents based on various criteria including document information and courses.
     *
     * @param filterRequest DTO containing filter criteria
     * @param pageable Pagination information
     * @return Page containing the list of documents matching the filter criteria
     */
    Page<DocumentResponseDto> filterDocuments(DocumentFilterRequest filterRequest, Pageable pageable);
}
