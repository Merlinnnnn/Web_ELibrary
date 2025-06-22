package com.spkt.libraSys.service.document.DigitalDocument;

import lombok.Data;

import java.util.List;

@Data
public class DigitalDocumentResponseDto {
    private Long digitalDocumentId;
    private String documentName;
    private String author;
    private String publisher;
    private String description;
    private String coverImage;
    private String approvalStatus;
    private String visibilityStatus;
    private List<UploadInfo> uploads;

    @Data
    public static class UploadInfo {
        private Long uploadId;
        private String fileName;
        private String fileType;
        private String filePath;
        private String uploadedAt;
    }
}