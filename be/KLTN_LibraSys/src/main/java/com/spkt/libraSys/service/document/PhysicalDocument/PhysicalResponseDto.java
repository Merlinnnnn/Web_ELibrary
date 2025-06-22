package com.spkt.libraSys.service.document.PhysicalDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhysicalResponseDto {

    private Long physicalDocumentId;
    private String documentName;
    private String author;
    private String publisher;
    private String description;
    private String coverImage;
    private String isbn;
    private int quantity; 
    private int borrowedCount; 
    private int unavailableCount; 
    private int availableCopies; 

}
