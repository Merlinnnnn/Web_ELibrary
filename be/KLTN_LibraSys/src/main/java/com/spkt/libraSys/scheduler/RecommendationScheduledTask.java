package com.spkt.libraSys.scheduler;

import com.spkt.libraSys.service.recommendation.MLRecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecommendationScheduledTask {
    private final MLRecommendationService mlRecommendationService;
    
    @Scheduled(cron = "10 0 20 * * ?")
    @Transactional
    public void runRecommendation() {
        LocalDateTime now = LocalDateTime.now();
        String formattedTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        log.info("Starting recommendation update at {}", formattedTime);
        
        try {
            mlRecommendationService.updateModel();
            log.info("Successfully completed recommendation update at {}", formattedTime);
        } catch (Exception e) {
            log.error("Error running recommendation update at {}: {}", formattedTime, e.getMessage());
            log.error("Stack trace:", e);
            // Có thể thêm logic để gửi thông báo lỗi qua email hoặc hệ thống monitoring
        }
    }
}
