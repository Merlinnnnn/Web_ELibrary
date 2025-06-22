package com.spkt.libraSys.service.notification;


import com.spkt.libraSys.service.user.UserEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    Page<NotificationEntity> findByUserOrderByCreatedAtDesc(UserEntity user, Pageable pageable);
    Page<NotificationEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Long countByUserAndStatus(UserEntity user, NotificationEntity.NotificationStatus status);
    
    @Modifying
    @Query("UPDATE notification n SET n.status = 'READ' WHERE n.user.userId = :userId")
    void markAllAsReadForUser(@Param("userId") String userId);
}