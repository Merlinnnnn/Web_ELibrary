package com.spkt.libraSys.service.drm;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class DrmCallbackService {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final DrmSessionRepository sessionRepository;
    private final DrmLicenseRepository licenseRepository;
    
    /**
     * Thông báo cho tất cả các client đang mở tài liệu về việc thu hồi
     */
    public void notifyRevocation(Long uploadId) {
        try {
            // Tìm tất cả các session liên quan đến tài liệu
            List<DrmLicenseEntity> licenses = licenseRepository.findByUploadIdAndRevoked(uploadId, true);
            
            for (DrmLicenseEntity license : licenses) {
                List<DrmSessionEntity> sessions = sessionRepository.findByLicenseId(license.getId());
                
                for (DrmSessionEntity session : sessions) {
                    // Gửi thông báo qua WebSocket
                    messagingTemplate.convertAndSendToUser(
                            license.getUserId(),
                            "/queue/drm-notifications",
                            new DrmNotification("REVOKED", uploadId, session.getSessionToken())
                    );
                }
            }
            
            log.info("Notification sent for document revocation: {}", uploadId);
        } catch (Exception e) {
            log.error("Error sending revocation notification", e);
        }
    }
}