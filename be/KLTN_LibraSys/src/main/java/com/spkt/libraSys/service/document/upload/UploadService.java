package com.spkt.libraSys.service.document.upload;

import com.spkt.libraSys.service.document.DigitalDocument.DigitalDocumentEntity;
import com.spkt.libraSys.service.document.DocumentEntity;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

public interface UploadService {
    void uploadImage(MultipartFile file, DocumentEntity docEntity);
    Set<UploadEntity> uploadFiles(List<MultipartFile> request);
    Set<UploadEntity> uploadFiles(List<MultipartFile> request, DigitalDocumentEntity digitalDocument);
    void deleteFiles(Set<UploadEntity> uploads);

    byte[] getDecryptedDocument(Long uploadId, String userId) throws Exception;

    FileStreamResponse getFileAsStream(Long uploadId) throws Exception;

}
