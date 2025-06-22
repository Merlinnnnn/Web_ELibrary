package com.spkt.libraSys.util;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "encryption_keys")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EncryptionKeyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "upload_id")
    private Long uploadId;

    @Column(name = "encryption_key")
    private String encryptionKey;
}