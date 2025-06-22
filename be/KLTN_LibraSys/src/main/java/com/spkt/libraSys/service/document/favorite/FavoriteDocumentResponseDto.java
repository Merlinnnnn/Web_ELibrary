package com.spkt.libraSys.service.document.favorite;

import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteDocumentResponseDto {

    private Long documentId;
    private String documentName;
    private String author;
    private String coverImage;
    private String documentCategory;
    private LocalDate publishedDate;
}
