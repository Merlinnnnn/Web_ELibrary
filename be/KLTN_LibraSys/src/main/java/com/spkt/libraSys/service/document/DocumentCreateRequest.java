package com.spkt.libraSys.service.document;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Builder
public class DocumentCreateRequest  {
//    @NotBlank(message = "ISBN is required")
    private String isbn;

    @NotBlank(message = "Document name is required")
    private String documentName;

    @NotBlank(message = "Author is required")
    private String author;

    private String publisher;

    private LocalDate publishedDate;

    private String language;

    @Min(value = 0, message = "Price must be at least 0")
    private double price;

    @Min(value = 0, message = "Quantity must be at least 0")
    private int quantity;


    // Mô tả ngắn về tài liệu
    private String description;


    @NotNull(message = "Document type IDs are required")
    private Set<Long> documentTypeIds;

   // @NotNull(message = "Course IDs are required")
    private Set<Long> courseIds;

    // Thêm trường MultipartFile cho coverImage
    private MultipartFile coverImage;

    // Đường dẫn tới tài liệu điện tử (nếu có)
    private List<MultipartFile> files; // File PDF
}
