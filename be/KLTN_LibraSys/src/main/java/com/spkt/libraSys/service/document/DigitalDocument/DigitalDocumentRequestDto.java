package com.spkt.libraSys.service.document.DigitalDocument;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;
import lombok.Data;
import java.util.List;
import java.util.Set;

@Data
public class DigitalDocumentRequestDto {

    @NotBlank(message = "Document name is required")
    private String documentName;

    @NotBlank(message = "Author is required")
    private String author;

    @NotBlank(message = "Publisher is required")
    private String publisher;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotNull(message = "Document type IDs are required")
    private Set<Long> documentTypeIds;

    private Set<Long> courseIds;

    private MultipartFile coverImage;

    @NotNull(message = "Files are required")
    private List<MultipartFile> files;
}
