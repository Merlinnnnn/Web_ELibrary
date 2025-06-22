package com.spkt.libraSys.service.document.programClass;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class ProgramClassResponse {
    Long id;
    String year;
    int semester;
    int studentBatch;
    String departmentName;
    Set<String> courseCodes;  
}