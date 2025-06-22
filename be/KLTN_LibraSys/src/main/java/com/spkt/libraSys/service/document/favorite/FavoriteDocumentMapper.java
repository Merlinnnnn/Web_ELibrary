package com.spkt.libraSys.service.document.favorite;

import org.springframework.stereotype.Component;

@Component
public class FavoriteDocumentMapper {

    public FavoriteDocumentResponseDto toDto(FavoriteDocumentEntity entity) {
        var doc = entity.getDocument();

        return FavoriteDocumentResponseDto.builder()
                .documentId(doc.getDocumentId())
                .documentName(doc.getDocumentName())
                .author(doc.getAuthor())
                .coverImage(doc.getCoverImage())
                .documentCategory(doc.getDocumentCategory().toString())
                .publishedDate(doc.getPublishedDate())
                .build();
    }
}
