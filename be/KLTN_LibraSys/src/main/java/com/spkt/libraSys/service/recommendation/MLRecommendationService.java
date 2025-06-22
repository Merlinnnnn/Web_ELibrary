package com.spkt.libraSys.service.recommendation;

import com.spkt.libraSys.service.document.DocumentEntity;
import com.spkt.libraSys.service.user.UserEntity;
import com.spkt.libraSys.service.PageDTO;
import com.spkt.libraSys.service.document.DocumentResponseDto;
import org.springframework.data.domain.Pageable;
import java.util.Map;
import java.util.List;

/**
 * Service interface for machine learning-based recommendation system.
 * Provides functionality for training models, making predictions, and analyzing user preferences.
 */
public interface MLRecommendationService {
    /**
     * Trains the ML model based on historical data.
     * This method processes user interactions and document metadata to build the recommendation model.
     */
    void trainModel();

    /**
     * Predicts the probability of a user liking a specific book.
     * @param user The user to make prediction for
     * @param document The document to predict preference for
     * @return Probability score between 0 and 1
     */
    double predictUserPreference(UserEntity user, DocumentEntity document);

    /**
     * Retrieves a list of book recommendations based on ML predictions.
     * @param pageable Pagination parameters
     * @return Page of recommended documents
     */
    PageDTO<DocumentResponseDto> getMLRecommendations(Pageable pageable);

    /**
     * Analyzes correlations between books based on user behavior patterns.
     * @param book The book to find correlations for
     * @return Map of correlated books and their correlation scores
     */
    Map<DocumentEntity, Double> getBookCorrelations(DocumentEntity book);

    /**
     * Updates the ML model with new data.
     * This method incorporates recent user interactions and document metadata into the existing model.
     */
    void updateModel();

    /**
     * Analyzes reading trends using machine learning.
     * @return Map of trend categories and their corresponding scores
     */
    Map<String, Double> analyzeReadingTrendsML();

    /**
     * Finds similar users based on ML analysis of reading patterns.
     * @param user The user to find similar users for
     * @return List of similar users
     */
    List<UserEntity> findSimilarUsersML(UserEntity user);

    /**
     * Retrieves a list of recommended documents for the current user.
     * @param pageable Pagination parameters
     * @return PageDTO containing the list of recommended documents
     */
    PageDTO<DocumentResponseDto> getRecommendedDocumentsForUser(Pageable pageable);
} 