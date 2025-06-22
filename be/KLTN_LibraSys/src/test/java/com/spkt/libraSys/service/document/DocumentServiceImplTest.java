package com.spkt.libraSys.service.document;

import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;
import com.spkt.libraSys.service.access.AuthService;
import com.spkt.libraSys.service.document.DigitalDocument.DigitalDocumentEntity;
import com.spkt.libraSys.service.document.DigitalDocument.DigitalDocumentRepository;
import com.spkt.libraSys.service.document.DocumentType.DocumentTypeEntity;
import com.spkt.libraSys.service.document.DocumentType.DocumentTypeRepository;
import com.spkt.libraSys.service.document.PhysicalDocument.PhysicalDocumentEntity;
import com.spkt.libraSys.service.document.PhysicalDocument.PhysicalDocumentRepository;
import com.spkt.libraSys.service.document.briefDocs.DocumentTextExtractionService;
import com.spkt.libraSys.service.document.course.CourseEntity;
import com.spkt.libraSys.service.document.course.CourseRepository;
import com.spkt.libraSys.service.document.documentValidation.DocumentValidationService;
import com.spkt.libraSys.service.document.upload.UploadEntity;
import com.spkt.libraSys.service.document.upload.UploadService;
import com.spkt.libraSys.service.document.viewer.ApprovalStatus;
import com.spkt.libraSys.service.role.RoleService;
import com.spkt.libraSys.service.user.UserEntity;
import com.spkt.libraSys.service.user.UserRepository;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentValidationService validationService;
    @Mock
    private UploadService uploadService;
    @Mock
    private AuthService authService;
    @Mock
    private DocumentMapper documentMapper;
    @Mock
    private DigitalDocumentRepository digitalDocumentRepository;
    @Mock
    private PhysicalDocumentRepository physicalDocumentRepository;
    @Mock
    private DocumentTypeRepository documentTypeRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private RoleService roleService;
    @Mock
    private DocumentTextExtractionService documentTextExtractionService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private DocumentServiceImpl documentService;

    private UserEntity testUser;
    private DocumentEntity testDocument;
    private DocumentResponseDto testResponseDto;
    private DocumentCreateRequest testCreateRequest;
    private DocumentUpdateRequest testUpdateRequest;
    private DocumentTypeEntity testDocumentType;
    private CourseEntity testCourse;
    private PhysicalDocumentEntity testPhysicalDocument;
    private DigitalDocumentEntity testDigitalDocument;
    private UploadEntity testUpload;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = UserEntity.builder()
                .userId("test123")
                .username("testuser@example.com")
                .build();

        // Setup test document
        testDocument = DocumentEntity.builder()
                .documentId(1L)
                .documentName("Test Document")
                .author("Test Author")
                .publisher("Test Publisher")
                .description("Test Description")
                .approvalStatus(ApprovalStatus.PENDING)
                .status(DocumentStatus.ENABLED)
                .build();

        // Setup test response DTO
        testResponseDto = new DocumentResponseDto();
        testResponseDto.setDocumentId(1L);
        testResponseDto.setDocumentName("Test Document");
        testResponseDto.setAuthor("Test Author");
        testResponseDto.setPublisher("Test Publisher");
        testResponseDto.setDescription("Test Description");

        // Setup test create request
        testCreateRequest = DocumentCreateRequest.builder()
                .documentName("Test Document")
                .author("Test Author")
                .publisher("Test Publisher")
                .description("Test Description")
                .documentTypeIds(Set.of(1L))
                .courseIds(Set.of(1L))
                .isbn("1234567890")
                .quantity(10)
                .price(0.0)
                .build();

        // Setup test update request
        testUpdateRequest = new DocumentUpdateRequest();
        testUpdateRequest.setDocumentName("Updated Document");
        testUpdateRequest.setAuthor("Updated Author");
        testUpdateRequest.setPublisher("Updated Publisher");
        testUpdateRequest.setDescription("Updated Description");
        testUpdateRequest.setDocumentTypeIds(Set.of(1L));
        testUpdateRequest.setCourseIds(Set.of(1L));
        testUpdateRequest.setIsbn("0987654321");
        testUpdateRequest.setQuantity(20);

        // Setup test document type
        testDocumentType = DocumentTypeEntity.builder()
                .documentTypeId(1L)
                .typeName("Test Type")
                .build();

        // Setup test course
        testCourse = CourseEntity.builder()
                .courseId(1L)
                .courseCode("TEST101")
                .courseName("Test Course")
                .build();

        // Setup test physical document
        testPhysicalDocument = PhysicalDocumentEntity.builder()
                .physicalDocumentId(1L)
                .document(testDocument)
                .isbn("1234567890")
                .quantity(10)
                .build();

        // Setup test digital document
        testDigitalDocument = DigitalDocumentEntity.builder()
                .digitalDocumentId(1L)
                .document(testDocument)
                .user(testUser)
                .build();

        // Setup test upload
        testUpload = UploadEntity.builder()
                .uploadId(1L)
                .fileName("test.pdf")
                .build();
    }

    @Test
    void createDocument_Success() throws IOException {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(documentMapper.toDocument(any())).thenReturn(testDocument);
        when(documentTypeRepository.findAllById(any())).thenReturn(List.of(testDocumentType));
        when(courseRepository.findAllById(any())).thenReturn(List.of(testCourse));
        when(documentRepository.save(any())).thenReturn(testDocument);
        when(physicalDocumentRepository.save(any())).thenReturn(testPhysicalDocument);
        when(documentMapper.toDocumentResponse(any())).thenReturn(testResponseDto);

        // Act
        DocumentResponseDto response = documentService.createDocument(testCreateRequest);

        // Assert
        assertNotNull(response);
        assertEquals(testResponseDto.getDocumentId(), response.getDocumentId());
        verify(documentRepository, times(2)).save(any());
        verify(physicalDocumentRepository).save(any());
        verify(documentTextExtractionService).summarizeDoc(any(), anyBoolean());
    }

    @Test
    void updateDocument_Success() {
        // Arrange
        when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
        when(documentTypeRepository.findAllById(any())).thenReturn(List.of(testDocumentType));
        when(courseRepository.findAllById(any())).thenReturn(List.of(testCourse));
        when(documentRepository.save(any())).thenReturn(testDocument);
        when(documentMapper.toDocumentResponse(any())).thenReturn(testResponseDto);

        // Act
        DocumentResponseDto response = documentService.updateDocument(1L, testUpdateRequest);

        // Assert
        assertNotNull(response);
        assertEquals(testResponseDto.getDocumentId(), response.getDocumentId());
        verify(documentRepository).save(any());
        verify(documentTextExtractionService).summarizeDoc(any(), anyBoolean());
    }

    @Test
    void getDocumentById_Success() {
        // Arrange
        when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
        when(documentMapper.toDocumentResponse(any())).thenReturn(testResponseDto);

        // Act
        DocumentResponseDto response = documentService.getDocumentById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(testResponseDto.getDocumentId(), response.getDocumentId());
    }

    @Test
    void getDocumentById_NotFound() {
        // Arrange
        when(documentRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AppException.class, () -> documentService.getDocumentById(1L));
    }

    @Test
    void getAllDocuments_Success() {
        // Arrange
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.getName()).thenReturn("testuser@example.com");
        Pageable pageable = PageRequest.of(0, 10);
        List<DocumentEntity> documents = Collections.singletonList(testDocument);
        Page<DocumentEntity> documentPage = new PageImpl<>(documents, pageable, documents.size());

        when(userRepository.findByUsername(any())).thenReturn(Optional.of(testUser));
        when(roleService.isAdmin(any())).thenReturn(false);
        when(documentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(documentPage);
        when(documentMapper.toDocumentResponse(any())).thenReturn(testResponseDto);

        // Act
        Page<DocumentResponseDto> response = documentService.getAllDocuments(pageable);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        verify(documentRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void deleteDocument_Success() {
        // Arrange
        when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
        when(documentRepository.save(any())).thenReturn(testDocument);

        // Act
        documentService.deleteDocument(1L);

        // Assert
        assertEquals(DocumentStatus.DISABLED, testDocument.getStatus());
        verify(documentRepository).save(testDocument);
    }

    @Test
    void deleteDocumentsByIds_Success() {
        // Arrange
        List<Long> documentIds = List.of(1L, 2L);
        List<DocumentEntity> documents = Arrays.asList(
            DocumentEntity.builder().documentId(1L).status(DocumentStatus.ENABLED).build(),
            DocumentEntity.builder().documentId(2L).status(DocumentStatus.ENABLED).build()
        );

        when(documentRepository.findAllById(documentIds)).thenReturn(documents);
        when(documentRepository.saveAll(any())).thenReturn(documents);

        // Act
        documentService.deleteDocumentsByIds(documentIds);

        // Assert
        documents.forEach(doc -> assertEquals(DocumentStatus.DISABLED, doc.getStatus()));
        verify(documentRepository).saveAll(documents);
    }

    @Test
    void searchByTitle_Success() {
        // Arrange
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.getName()).thenReturn("testuser@example.com");
        String title = "Test";
        Pageable pageable = PageRequest.of(0, 10);
        List<DocumentEntity> documents = Collections.singletonList(testDocument);
        Page<DocumentEntity> documentPage = new PageImpl<>(documents, pageable, documents.size());

        when(userRepository.findByUsername(any())).thenReturn(Optional.of(testUser));
        when(roleService.isAdmin(any())).thenReturn(false);
        when(documentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(documentPage);
        when(documentMapper.toDocumentResponse(any())).thenReturn(testResponseDto);

        // Act
        Page<DocumentResponseDto> response = documentService.searchByTitle(title, pageable);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        verify(documentRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void filterDocuments_Success() {
        // Arrange
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.getName()).thenReturn("testuser@example.com");
        DocumentFilterRequest filterRequest = new DocumentFilterRequest();
        filterRequest.setDocumentName("Test");
        filterRequest.setAuthor("Test Author");
        filterRequest.setPublisher("Test Publisher");
        filterRequest.setLanguage("English");
        filterRequest.setDocumentCategory(DocumentCategory.PHYSICAL);
        filterRequest.setStatus(DocumentStatus.ENABLED);
        filterRequest.setApprovalStatus(ApprovalStatus.APPROVED);
        filterRequest.setPublishedDateFrom(LocalDate.now().minusDays(30));
        filterRequest.setPublishedDateTo(LocalDate.now());
        filterRequest.setCourseIds(Set.of(1L));
        filterRequest.setDocumentTypeIds(Set.of(1L));

        Pageable pageable = PageRequest.of(0, 10);
        List<DocumentEntity> documents = Collections.singletonList(testDocument);
        Page<DocumentEntity> documentPage = new PageImpl<>(documents, pageable, documents.size());

        when(userRepository.findByUsername(any())).thenReturn(Optional.of(testUser));
        when(roleService.isAdmin(any())).thenReturn(false);
        when(documentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(documentPage);
        when(documentMapper.toDocumentResponse(any())).thenReturn(testResponseDto);

        // Act
        Page<DocumentResponseDto> response = documentService.filterDocuments(filterRequest, pageable);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        verify(documentRepository).findAll(any(Specification.class), any(Pageable.class));
    }
} 