package com.spkt.libraSys.service.loan;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LoanMapper {

    //@Mapping(target = "documentName", source = ".documentName")
    @Mapping(target = "username", source = "userEntity.username")
    @Mapping(target = "documentName", source = "physicalDoc.document.documentName")
    @Mapping(target = "physicalDocId", source = "physicalDoc.physicalDocumentId")
    @Mapping(target = "documentId", source = "physicalDoc.document.documentId")
    LoanResponse toLoanTransactionResponse(LoanEntity loanTransaction);

}