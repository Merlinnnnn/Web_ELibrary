package com.spkt.libraSys.service.recommendation;


import com.spkt.libraSys.service.PageDTO;
import com.spkt.libraSys.service.document.DocumentResponseDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for document recommendations.
 * Provides functionality for retrieving personalized document recommendations for users.
 */
public interface RecommendationService {
    /**
     * Retrieves a list of recommended documents for the current user with pagination.
     *
     * @param pageable Pagination information
     * @return A PageDTO object containing the list of DocumentResponse
     */
    PageDTO<DocumentResponseDto> getRecommendedDocumentsForCurrentUser(Pageable pageable);
}