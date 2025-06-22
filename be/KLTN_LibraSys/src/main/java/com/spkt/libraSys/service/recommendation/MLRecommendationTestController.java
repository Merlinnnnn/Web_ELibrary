package com.spkt.libraSys.service.recommendation;

import com.spkt.libraSys.service.recommendation.MLRecommendationTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ml-test")
@RequiredArgsConstructor
public class MLRecommendationTestController {
    private final MLRecommendationTestService testService;

    @PostMapping("/run")
    public ResponseEntity<String> runTests() {
        testService.testRecommendationSystem();
        return ResponseEntity.ok("ML recommendation system tests completed. Check console for results.");
    }
} 