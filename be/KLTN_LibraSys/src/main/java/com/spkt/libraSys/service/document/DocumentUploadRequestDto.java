package com.spkt.libraSys.service.document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Data
public class DocumentUploadRequestDto {

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
    private Set<Long> documentTypeIds; 

    @NotNull(message = "Vui lòng chọn ít nhất một khóa học")
    private Set<Long> courseIds; 


    private MultipartFile coverImage;; 

   // @NotNull(message = "File tài liệu không được để trống")
   private List<MultipartFile> files; 
}
