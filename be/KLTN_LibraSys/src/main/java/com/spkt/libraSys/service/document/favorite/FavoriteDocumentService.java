package com.spkt.libraSys.service.document.favorite;

import com.spkt.libraSys.service.document.favorite.FavoriteDocumentResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for managing user's favorite documents.
 * Provides functionality for marking documents as favorites, retrieving favorite documents,
 * and checking favorite status.
 */
public interface FavoriteDocumentService {

    /**
     * Marks a document as favorite for the current user.
     * If the document is already marked as favorite, no action is taken.
     *
     * @param documentId ID of the document to mark as favorite
     * @throws RuntimeException if the document is not found
     */
    void markFavorite(Long documentId);

    /**
     * Removes a document from the current user's favorites.
     *
     * @param documentId ID of the document to remove from favorites
     * @throws RuntimeException if the document is not found
     */
    void unmarkFavorite(Long documentId);

    /**
     * Retrieves all favorite documents for the current user with pagination support.
     *
     * @param pageable Pagination and sorting information
     * @return Page of FavoriteDocumentResponseDto containing the user's favorite documents
     */
    Page<FavoriteDocumentResponseDto> getFavoriteDocuments(Pageable pageable);

    /**
     * Checks if a document is marked as favorite by the current user.
     *
     * @param documentId ID of the document to check
     * @return true if the document is marked as favorite, false otherwise
     * @throws RuntimeException if the document is not found
     */
    boolean isFavorite(Long documentId);

}
