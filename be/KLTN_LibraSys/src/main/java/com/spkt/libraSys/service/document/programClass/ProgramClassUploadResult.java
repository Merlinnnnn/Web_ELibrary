package com.spkt.libraSys.service.document.programClass;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProgramClassUploadResult {
    private int totalRows;
    private int inserted;
    private int skipped;
    private int duplicated;
}
