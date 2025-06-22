package com.spkt.libraSys.service.qrcode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/qr")
@RequiredArgsConstructor
public class QRController {

    private final QRService qrService;

    // 🔹 API Tạo mã QR cho giao dịch mượn sách
//    @GetMapping("/{transactionId}/generate")
//    public ResponseEntity<byte[]> getLoanQRCode(@PathVariable Long transactionId) {
//        String jwtToken = qrService.generateJwtToken(transactionId); // ✅ Tạo JWT Token
//        log.info("QR Image");
//        byte[] qrImage = qrService.generateQRCode(jwtToken); // ✅ Chuyển thành ảnh QR
//
//        if (qrImage == null) {
//            return ResponseEntity.badRequest().build();
//        }
//
//        return ResponseEntity.ok()
//                .contentType(MediaType.IMAGE_PNG)
//                .body(qrImage);
//    }

    // 🔹 API Quét mã QR để xác nhận nhận sách/trả sách
//    @PostMapping("/scan")
//    public ResponseEntity<String> scanQRCode(@RequestBody String qrData) {
//        String responseMessage = qrService.parseJwtToken(qrData);
//        return ResponseEntity.ok(responseMessage);
//    }
}
