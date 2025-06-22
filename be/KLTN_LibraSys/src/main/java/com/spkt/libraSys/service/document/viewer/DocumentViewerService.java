package com.spkt.libraSys.service.document.viewer;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Service interface for viewing and streaming document content.
 * Provides functionality for retrieving document pages, streaming video content,
 * and accessing document metadata.
 */
public interface DocumentViewerService {
    /**
     * Retrieves the content of a specific page from a document.
     * @param uploadId The ID of the uploaded document
     * @param pageNumber The page number to retrieve
     * @return The content of the requested page as a byte array
     */
    byte[] getDocumentPageContent(Long uploadId, int pageNumber);

    /**
     * Gets the total number of pages in a document.
     * @param uploadId The ID of the uploaded document
     * @return The total number of pages
     */
    int getDocumentPageCount(Long uploadId);

    /**
     * Streams video content with support for range requests.
     * @param uploadId The ID of the uploaded video
     * @param rangeHeader The HTTP range header for partial content requests
     * @return ResponseEntity containing the video resource
     * @throws IOException if there's an error accessing the video file
     */
    ResponseEntity<Resource> streamVideo(Long uploadId, String rangeHeader) throws IOException;

    /**
     * Retrieves the full content of a document.
     * @param uploadId The ID of the uploaded document
     * @return The document resource
     * @throws IOException if there's an error accessing the document
     */
    Resource getFullDocumentContent(Long uploadId) throws IOException;

    /**
     * Gets the original filename of an uploaded document.
     * @param uploadId The ID of the uploaded document
     * @return The original filename
     */
    String getFileName(Long uploadId);

    /**
     * Gets the content type (MIME type) of an uploaded document.
     * @param uploadId The ID of the uploaded document
     * @return The content type of the document
     */
    String getFileContentType(Long uploadId);
}
