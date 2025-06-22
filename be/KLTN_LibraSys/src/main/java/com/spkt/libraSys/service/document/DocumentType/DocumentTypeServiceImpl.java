package com.spkt.libraSys.service.document.DocumentType;

import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.DocumentType;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DocumentTypeServiceImpl implements DocumentTypeService {

    DocumentTypeRepository documentTypeRepository;
    DocumentTypeMapper documentTypeMapper;

    @Override
    public DocumentTypeResponse getIdDocumentType(Long documentTypeId) {
        return documentTypeMapper.toDocumentTypeResponse(documentTypeRepository.findById(documentTypeId)
                .orElseThrow(()-> new AppException(ErrorCode.DOCUMENT_TYPE_NOT_FOUND)));

    }

    @Override
    public Page<DocumentTypeResponse> getAllDocumentTypes(Pageable pageable) {
        return documentTypeRepository.findAll(pageable)
                .map(documentTypeMapper::toDocumentTypeResponse);
    }

    @Override
    @Transactional
    public DocumentTypeResponse createDocumentType(DocumentTypeRequestDto request) {
        DocumentTypeEntity documentType = documentTypeMapper.toDocumentType(request);
        DocumentTypeEntity savedDocumentType = documentTypeRepository.save(documentType);
        return documentTypeMapper.toDocumentTypeResponse(savedDocumentType);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public DocumentTypeResponse updateDocumentType(Long id, DocumentTypeRequestDto request) {
        DocumentTypeEntity documentType = documentTypeRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_TYPE_NOT_FOUND));
        documentType.setTypeName(request.getTypeName());
        DocumentTypeEntity updatedDocumentType = documentTypeRepository.save(documentType);
        return documentTypeMapper.toDocumentTypeResponse(updatedDocumentType);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteDocumentType(Long id) {
        DocumentTypeEntity documentType = documentTypeRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_TYPE_NOT_FOUND));
        documentTypeRepository.delete(documentType);
    }
}