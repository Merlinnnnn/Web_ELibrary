package com.spkt.libraSys.service.document.DigitalDocument.AccessRequest;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.spkt.libraSys.service.document.DocumentEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "access_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AccessRequestEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String coverImage;
    private Long digitalId;
    private String requesterId; // người xin phép
    private String ownerId;     // người sở hữu tài liệu
    @Enumerated(EnumType.STRING)
    private AccessRequestStatus status; // PENDING, APPROVED, REJECTED
    private LocalDateTime requestTime;
    private LocalDateTime decisionTime;
    private String reviewerId;  // người duyệt

    private LocalDateTime licenseExpiry;
}
