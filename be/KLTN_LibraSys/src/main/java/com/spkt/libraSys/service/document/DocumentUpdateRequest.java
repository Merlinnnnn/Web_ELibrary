package com.spkt.libraSys.service.document;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class DocumentUpdateRequest {
    private String isbn;
    @NotBlank(message = "Document name is required")
    private String documentName;

    private String author;
    private String publisher;
    private LocalDate publishedDate;
    private String language;

    @Min(value = 0, message = "Quantity must be at least 0")
    private int quantity;

    private String description;

    private Set<Long> documentTypeIds;
    private Set<Long> courseIds;

    private MultipartFile coverImage;
    private List<MultipartFile> files;
}
