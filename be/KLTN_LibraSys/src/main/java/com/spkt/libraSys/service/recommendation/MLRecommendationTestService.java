package com.spkt.libraSys.service.recommendation;

import com.spkt.libraSys.service.document.DocumentEntity;
import com.spkt.libraSys.service.document.DocumentResponseDto;
import com.spkt.libraSys.service.user.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MLRecommendationTestService {
    private final MLRecommendationService mlRecommendationService;

    public void testRecommendationSystem() {
        // 1. Train model
        System.out.println("Training model...");
        mlRecommendationService.trainModel();
        System.out.println("Model training completed.");

        // 2. Test user preference prediction
        System.out.println("\nTesting user preference prediction...");
        UserEntity testUser = new UserEntity(); // Replace with actual user
        DocumentEntity testDoc = new DocumentEntity(); // Replace with actual document
        double preference = mlRecommendationService.predictUserPreference(testUser, testDoc);
        System.out.println("User preference score: " + preference);

        // 3. Test book correlations
        System.out.println("\nTesting book correlations...");
        Map<DocumentEntity, Double> correlations = mlRecommendationService.getBookCorrelations(testDoc);
        System.out.println("Found " + correlations.size() + " correlated books");

        // 4. Test reading trends
        System.out.println("\nTesting reading trends...");
        Map<String, Double> trends = mlRecommendationService.analyzeReadingTrendsML();
        System.out.println("Reading trends by category:");
        trends.forEach((category, score) -> 
            System.out.println(category + ": " + score));

        // 5. Test similar users
        System.out.println("\nTesting similar users...");
        List<UserEntity> similarUsers = mlRecommendationService.findSimilarUsersML(testUser);
        System.out.println("Found " + similarUsers.size() + " similar users");
    }
} 