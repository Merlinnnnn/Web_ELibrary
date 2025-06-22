package com.spkt.libraSys.service.document.DocumentType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DocumentTypeRequestDto {
    @NotBlank(message = "Tên loại tài liệu không được để trống")
    @Size(max = 100, message = "Tên loại tài liệu không được vượt quá 100 ký tự")
    private String typeName;

    @Size(max = 100, message = "Mô tả không được vượt quá 500 ký tự")
    private String description;
}