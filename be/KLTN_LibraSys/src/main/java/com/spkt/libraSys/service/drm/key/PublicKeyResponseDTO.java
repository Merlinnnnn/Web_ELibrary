package com.spkt.libraSys.service.drm.key;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicKeyResponseDTO {
    private String publicKey;        // Chuỗi public key được mã hóa Base64
    private String keyId;            // Định danh của cặp khóa
    private LocalDateTime createdAt;  // Thời điểm tạo khóa
    private LocalDateTime expiresAt;  // Thời điểm hết hạn
    private String algorithm;        // Thuật toán sử dụng (RSA)
    private Integer keySize;         // Kích thước khóa (2048 bit)
}
