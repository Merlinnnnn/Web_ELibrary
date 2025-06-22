package com.spkt.libraSys.service.qrcode;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Component
public class QrCodeUtil {

    @Value("${jwt.signer-key}") // 🔒 Khóa bí mật JWT từ config
    private String SIGNER_KEY;

    private static final int QR_SIZE = 300; // Kích thước QR Code

    // 🔹 Tạo JWT Token chứa transactionId
    public String generateJwtToken(Long transactionId) {
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SIGNER_KEY));

        return Jwts.builder()
                .setId(transactionId.toString()) // ✅ Lưu transactionId vào JWT
                .setIssuer("LibraSys")
                .setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plusSeconds(900))) // Token hết hạn sau 15 phút
                .signWith(key, SignatureAlgorithm.HS512) // ✅ Sử dụng thuật toán bảo mật HS512
                .compact();
    }

    // 🔹 Giải mã JWT Token để lấy transactionId
    public Long parseJwtToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SIGNER_KEY));

            return Long.parseLong(
                    Jwts.parser()
                            .setSigningKey(key)
                            .build()
                            .parseClaimsJws(token)
                            .getBody()
                            .getId() // ✅ Lấy transactionId từ JWT
            );
        } catch (Exception e) {
            log.error("❌ Lỗi khi giải mã JWT Token", e);
            return null;
        }
    }

    // 🔹 Tạo mã QR từ JWT Token
    public byte[] generateQRCode(String jwtToken) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(jwtToken, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            return pngOutputStream.toByteArray(); // ✅ Trả về ảnh QR Code dưới dạng byte[]
        } catch (WriterException | IOException e) {
            log.error("❌ Lỗi khi tạo mã QR", e);
            return null;
        }
    }

    // 🔹 Xác minh JWT Token từ mã QR quét được
    public boolean verifyQRCode(String qrData) {
        try {
            Long transactionId = parseJwtToken(qrData);
            return transactionId != null;
        } catch (Exception e) {
            log.error("❌ Mã QR không hợp lệ", e);
            return false;
        }
    }
}
