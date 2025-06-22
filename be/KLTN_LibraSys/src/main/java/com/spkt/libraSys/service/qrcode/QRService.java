package com.spkt.libraSys.service.qrcode;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.spkt.libraSys.config.CustomUserDetails;
import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;
import com.spkt.libraSys.service.access.AuthService;
import com.spkt.libraSys.service.loan.LoanEntity;
import com.spkt.libraSys.service.loan.LoanRepository;
import com.spkt.libraSys.service.loan.LoanStatus;
import com.spkt.libraSys.service.user.UserEntity;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.record.UserSViewEnd;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QRService {

    @NonFinal
    @Value("${jwt.signer-key}") // 🔒 Khóa bí mật JWT từ application.yml
    private String SIGNER_KEY;

    private static final int QR_SIZE = 300; // Kích thước QR Code

    // 🔹 Tạo JWT Token chứa transactionId (dùng để tạo QR Code)
    public String generateJwtTokenLoan(Long transactionId,LoanStatus status) {
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SIGNER_KEY));

        return Jwts.builder()
                .id(transactionId.toString()) // ✅ Lưu transactionId vào JWT
                .issuer("LibraSys")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(900))) // Token hết hạn sau 15 phút
                .claim("status", status.name())
                .signWith(key) // ✅ Sử dụng thuật toán bảo mật HS512
                .compact();
    }

    // 🔹 Tạo token xác thực người quét
    public String generateScannerToken(String userId) {
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SIGNER_KEY));

        return Jwts.builder()
                .claim("userId", userId)
                .issuer("LibraSys")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(900)))
                .signWith(key)
                .compact();
    }

    // 🔹 Giải mã JWT Token để lấy transactionId
    public JwtTokenData parseJwtToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SIGNER_KEY));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long transactionId = Long.parseLong(claims.getId()); // ✅ Lấy transactionId từ JWT
            LoanStatus loanStatus = LoanStatus.valueOf(claims.get("status", String.class));
            Date expiration = claims.getExpiration();
            boolean isExpired = expiration.before(new Date());
            if (isExpired) {
                throw new AppException(ErrorCode.TOKEN_EXPIRED, "Token đã hết hạn vào: " + expiration);
            }
            return new JwtTokenData(transactionId,loanStatus );
        } catch (JwtException e) {
            throw new AppException(ErrorCode.TOKEN_EXPIRED, "Token khong hop le");
        }
    }

    // 🔹 Tạo mã QR từ JWT Token
    public byte[] generateQRCode(String jwtToken) {
        try {
            BitMatrix bitMatrix = new com.google.zxing.qrcode.QRCodeWriter()
                    .encode(jwtToken, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            return pngOutputStream.toByteArray(); // ✅ Trả về ảnh QR Code dưới dạng byte[]
        } catch (WriterException | IOException e) {
            log.error("❌ Lỗi khi tạo mã QR", e);
            return null;
        }
    }
}
