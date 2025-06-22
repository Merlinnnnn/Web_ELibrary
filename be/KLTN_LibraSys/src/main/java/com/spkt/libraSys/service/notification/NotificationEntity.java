package com.spkt.libraSys.service.notification;

import com.spkt.libraSys.service.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(name = "notification")
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    UserEntity user;

    @Column(nullable = false)
    String title;

    @Column(nullable = false)
    String content;

    @Column(nullable = true)
    String entityId;
    @Column(nullable = true)
    String entityType;

    @Column(nullable = false)
    LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    NotificationStatus status; // UNREAD, READ


    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
    public enum NotificationStatus {
        UNREAD, READ
    }

}