package com.spkt.libraSys.service.document.briefDocs;

import com.spkt.libraSys.exception.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/documents/summarize")
@RequiredArgsConstructor
public class DocumentSummarizeController {

    private final DocumentTextExtractionService documentTextExtractionService;

    @PostMapping("/{documentId}")
    public ResponseEntity<ApiResponse<String>> summarizeDocument(@PathVariable Long documentId) {
        documentTextExtractionService.summarizeDoc(documentId,true);

        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .message("Yêu cầu tóm tắt tài liệu đã được ghi nhận và đang xử lý.")
                        .build()
        );
    }
}
