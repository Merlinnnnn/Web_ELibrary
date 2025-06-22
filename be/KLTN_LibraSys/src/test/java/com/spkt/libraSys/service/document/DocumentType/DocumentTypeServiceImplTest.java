package com.spkt.libraSys.service.document.DocumentType;

import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentTypeServiceImplTest {

    @Mock
    private DocumentTypeRepository documentTypeRepository;

    @Mock
    private DocumentTypeMapper documentTypeMapper;

    @InjectMocks
    private DocumentTypeServiceImpl documentTypeService;

    private DocumentTypeEntity testDocumentType;
    private DocumentTypeResponse testResponse;
    private DocumentTypeRequestDto testRequest;

    @BeforeEach
    void setUp() {
        // Setup test document type entity
        testDocumentType = DocumentTypeEntity.builder()
                .documentTypeId(1L)
                .typeName("Test Document Type")
                .description("Test Description")
                .build();

        // Setup test response
        testResponse = DocumentTypeResponse.builder()
                .documentTypeId(1L)
                .typeName("Test Document Type")
                .build();

        // Setup test request
        testRequest = new DocumentTypeRequestDto();
        testRequest.setTypeName("Test Document Type");
        testRequest.setDescription("Test Description");
    }

    @Test
    void getIdDocumentType_Success() {
        when(documentTypeRepository.findById(1L)).thenReturn(Optional.of(testDocumentType));
        when(documentTypeMapper.toDocumentTypeResponse(testDocumentType)).thenReturn(testResponse);

        DocumentTypeResponse result = documentTypeService.getIdDocumentType(1L);

        assertNotNull(result);
        assertEquals(testResponse.getDocumentTypeId(), result.getDocumentTypeId());
        assertEquals(testResponse.getTypeName(), result.getTypeName());
    }

    @Test
    void getIdDocumentType_NotFound() {
        when(documentTypeRepository.findById(1L)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, 
            () -> documentTypeService.getIdDocumentType(1L));
        assertEquals(ErrorCode.DOCUMENT_TYPE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void getAllDocumentTypes_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<DocumentTypeEntity> documentTypePage = new PageImpl<>(Collections.singletonList(testDocumentType));

        when(documentTypeRepository.findAll(pageable)).thenReturn(documentTypePage);
        when(documentTypeMapper.toDocumentTypeResponse(any(DocumentTypeEntity.class)))
            .thenReturn(testResponse);

        Page<DocumentTypeResponse> result = documentTypeService.getAllDocumentTypes(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testResponse.getDocumentTypeId(), result.getContent().get(0).getDocumentTypeId());
        assertEquals(testResponse.getTypeName(), result.getContent().get(0).getTypeName());
    }

    @Test
    void createDocumentType_Success() {
        when(documentTypeMapper.toDocumentType(testRequest)).thenReturn(testDocumentType);
        when(documentTypeRepository.save(any(DocumentTypeEntity.class))).thenReturn(testDocumentType);
        when(documentTypeMapper.toDocumentTypeResponse(testDocumentType)).thenReturn(testResponse);

        DocumentTypeResponse result = documentTypeService.createDocumentType(testRequest);

        assertNotNull(result);
        assertEquals(testResponse.getDocumentTypeId(), result.getDocumentTypeId());
        assertEquals(testResponse.getTypeName(), result.getTypeName());
        verify(documentTypeRepository).save(any(DocumentTypeEntity.class));
    }

    @Test
    void updateDocumentType_Success() {
        when(documentTypeRepository.findById(1L)).thenReturn(Optional.of(testDocumentType));
        when(documentTypeRepository.save(any(DocumentTypeEntity.class))).thenReturn(testDocumentType);
        when(documentTypeMapper.toDocumentTypeResponse(testDocumentType)).thenReturn(testResponse);

        DocumentTypeResponse result = documentTypeService.updateDocumentType(1L, testRequest);

        assertNotNull(result);
        assertEquals(testResponse.getDocumentTypeId(), result.getDocumentTypeId());
        assertEquals(testResponse.getTypeName(), result.getTypeName());
        verify(documentTypeRepository).save(any(DocumentTypeEntity.class));
    }

    @Test
    void updateDocumentType_NotFound() {
        when(documentTypeRepository.findById(1L)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, 
            () -> documentTypeService.updateDocumentType(1L, testRequest));
        assertEquals(ErrorCode.DOCUMENT_TYPE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void deleteDocumentType_Success() {
        when(documentTypeRepository.findById(1L)).thenReturn(Optional.of(testDocumentType));
        doNothing().when(documentTypeRepository).delete(testDocumentType);

        assertDoesNotThrow(() -> documentTypeService.deleteDocumentType(1L));
        verify(documentTypeRepository).delete(testDocumentType);
    }

    @Test
    void deleteDocumentType_NotFound() {
        when(documentTypeRepository.findById(1L)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, 
            () -> documentTypeService.deleteDocumentType(1L));
        assertEquals(ErrorCode.DOCUMENT_TYPE_NOT_FOUND, exception.getErrorCode());
    }
} 