package com.spkt.libraSys.scheduler;

import com.spkt.libraSys.service.drm.DrmSessionEntity;
import com.spkt.libraSys.service.drm.DrmSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class drmScheduledTasks {

    @Autowired
    DrmSessionRepository drmSessionRepository;

    @Scheduled(fixedDelay = 2 * 60 * 1000)
    public void checkInactiveSessions() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(3); // timeout sau 3 phút

        List<DrmSessionEntity> activeSessions = drmSessionRepository.findByActive(true);

        for (DrmSessionEntity session : activeSessions) {
            if (session.getLastHeartbeat().isBefore(threshold)) {
                session.setActive(false);
                drmSessionRepository.save(session);
                log.info("Session {} marked as inactive due to timeout", session.getSessionToken());
            }
        }
    }

}
