package com.spkt.libraSys.service.document.viewer;

import com.spkt.libraSys.exception.ApiResponse;
import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;
import com.spkt.libraSys.service.access.AuthService;
import com.spkt.libraSys.service.document.DigitalDocument.DigitalDocumentService;
import com.spkt.libraSys.service.user.UserEntity;
import org.springframework.core.io.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentViewerController {

    private final DocumentViewerService documentViewerService;
    private final DigitalDocumentService digitalDocumentService;
    private final AuthService authService;

    // Xem một trang tài liệu
    @GetMapping(value = "/view/{uploadId}/{pageNumber}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> viewDocumentPage(@PathVariable Long uploadId, @PathVariable int pageNumber) {
        byte[] pageContent = documentViewerService.getDocumentPageContent(uploadId, pageNumber);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(pageContent);
    }

    // Đọc nội dung tài liệu
    @GetMapping(value = "/read/{uploadId}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<byte[]> readWordDocument(@PathVariable Long uploadId) {
        byte[] content = documentViewerService.getDocumentPageContent(uploadId, 1); // Đọc trang đầu tiên
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(content);
    }

    // Lấy số trang của tài liệu
    @GetMapping("/page-count/{uploadId}")
    public ResponseEntity<ApiResponse<Integer>> getPageCount(@PathVariable Long uploadId) {
        int pageCount = documentViewerService.getDocumentPageCount(uploadId);
        return ResponseEntity.ok(
                ApiResponse.<Integer>builder()
                        .message("Lấy số trang thành công")
                        .data(pageCount)
                        .build()
        );
    }

    // Stream video
    @GetMapping(value = "/stream/{uploadId}", produces = "video/mp4")
    public ResponseEntity<Resource> streamVideo(@PathVariable Long uploadId, @RequestHeader(value = "Range", required = false) String rangeHeader) throws IOException {
        return documentViewerService.streamVideo(uploadId, rangeHeader);
    }

    // Tải tài liệu về máy
    @GetMapping("/download/{uploadId}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long uploadId) throws IOException {
        UserEntity userEntity = authService.getCurrentUser();
        if(!digitalDocumentService.hasPermissionToAccess(uploadId,userEntity)){
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền tải tài liệu này.");

        }
        Resource file = documentViewerService.getFullDocumentContent(uploadId);
        String fileName = documentViewerService.getFileName(uploadId);
        String contentType = documentViewerService.getFileContentType(uploadId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(file);
    }
}
