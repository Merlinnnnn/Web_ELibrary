package com.spkt.libraSys.service.document.PhysicalDocument;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PhysicalRequestDto {
    @NotBlank(message = "ISBN không được để trống")
    private String isbn;

    @NotBlank(message = "Tên tài liệu không được để trống")
    private String documentName;

    @NotBlank(message = "Tên tác giả không được để trống")
    private String author;

    private int quantity;

    private String publisher;

    private String description;

    @NotNull(message = "Vui lòng chọn ít nhất một danh mục tài liệu")
    private Set<Long> documentTypeIds; // Danh mục tài liệu

    @NotNull(message = "Vui lòng chọn ít nhất một khóa học")
    private Set<Long> courseIds; // Khóa học liên quan

    private MultipartFile coverImage;; // Ảnh bìa tài liệu (tùy chọn)


 }
