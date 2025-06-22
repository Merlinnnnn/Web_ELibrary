package com.spkt.libraSys.service.document.DigitalDocument;

import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;
import com.spkt.libraSys.service.access.AuthService;
import com.spkt.libraSys.service.document.DocumentEntity;
import com.spkt.libraSys.service.document.DocumentFilterRequest;
import com.spkt.libraSys.service.document.DocumentRepository;
import com.spkt.libraSys.service.document.DocumentType.DocumentTypeEntity;
import com.spkt.libraSys.service.document.DocumentType.DocumentTypeRepository;
import com.spkt.libraSys.service.document.briefDocs.DocumentTextExtractionService;
import com.spkt.libraSys.service.document.course.CourseEntity;
import com.spkt.libraSys.service.document.course.CourseRepository;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DigitalDocumentServiceImplTest {

    @Mock
    private DigitalDocumentRepository digitalDocumentRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private UploadService uploadService;
    @Mock
    private DigitalDocumentMapper digitalDocumentMapper;
    @Mock
    private AuthService authService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleService roleService;
    @Mock
    private DocumentTextExtractionService documentTextExtractionService;
    @Mock
    private DocumentTypeRepository documentTypeRepository;
    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private DigitalDocumentServiceImpl digitalDocumentService;

    private UserEntity testUser;
    private DocumentEntity testDocument;
    private DigitalDocumentEntity testDigitalDocument;
    private DigitalDocumentResponseDto testResponseDto;
    private DigitalDocumentRequestDto testRequestDto;
    private DocumentTypeEntity testDocumentType;
    private CourseEntity testCourse;
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
                .build();

        // Setup test digital document
        testDigitalDocument = DigitalDocumentEntity.builder()
                .digitalDocumentId(1L)
                .document(testDocument)
                .user(testUser)
                .visibilityStatus(VisibilityStatus.RESTRICTED_VIEW)
                .build();

        // Setup test response DTO
        testResponseDto = new DigitalDocumentResponseDto();
        testResponseDto.setDigitalDocumentId(1L);
        testResponseDto.setDocumentName("Test Document");
        testResponseDto.setAuthor("Test Author");
        testResponseDto.setPublisher("Test Publisher");
        testResponseDto.setDescription("Test Description");
        testResponseDto.setVisibilityStatus(VisibilityStatus.RESTRICTED_VIEW.name());

        // Setup test request DTO
        testRequestDto = new DigitalDocumentRequestDto();
        testRequestDto.setDocumentName("Test Document");
        testRequestDto.setAuthor("Test Author");
        testRequestDto.setPublisher("Test Publisher");
        testRequestDto.setDescription("Test Description");
        testRequestDto.setDocumentTypeIds(Set.of(1L));
        testRequestDto.setCourseIds(Set.of(1L));

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

        // Setup test upload
        testUpload = UploadEntity.builder()
                .uploadId(1L)
                .fileName("test.pdf")
                .build();
    }

    @Test
    void createDigitalDocument_Success() throws IOException {
        // Arrange
        List<MultipartFile> testFiles = List.of(
            new MockMultipartFile("test.pdf", "test.pdf", "application/pdf", "test content".getBytes())
        );
        testRequestDto.setFiles(testFiles);

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(digitalDocumentMapper.toDocumentEntity(any())).thenReturn(testDocument);
        when(documentTypeRepository.findAllById(any())).thenReturn(List.of(testDocumentType));
        when(courseRepository.findAllById(any())).thenReturn(List.of(testCourse));
        when(documentRepository.save(any())).thenReturn(testDocument);
        when(digitalDocumentRepository.save(any())).thenReturn(testDigitalDocument);
        when(uploadService.uploadFiles(anyList(), any(DigitalDocumentEntity.class))).thenReturn(Set.of(testUpload));
        when(digitalDocumentMapper.toResponseDto(any())).thenReturn(testResponseDto);

        // Act
        DigitalDocumentResponseDto response = digitalDocumentService.createDigitalDocument(testRequestDto);

        // Assert
        assertNotNull(response);
        assertEquals(testResponseDto.getDigitalDocumentId(), response.getDigitalDocumentId());
        verify(documentRepository).save(any());
        verify(digitalDocumentRepository, times(2)).save(any());
        verify(documentTextExtractionService).summarizeDoc(any(), anyBoolean());
        verify(uploadService).uploadFiles(anyList(), any(DigitalDocumentEntity.class));
    }

    @Test
    void getDigitalDocument_Success() {
        // Arrange
        when(digitalDocumentRepository.findById(1L)).thenReturn(Optional.of(testDigitalDocument));
        when(digitalDocumentMapper.toResponseDto(any())).thenReturn(testResponseDto);

        // Act
        DigitalDocumentResponseDto response = digitalDocumentService.getDigitalDocument(1L);

        // Assert
        assertNotNull(response);
        assertEquals(testResponseDto.getDigitalDocumentId(), response.getDigitalDocumentId());
        verify(digitalDocumentRepository).findById(1L);
    }

    @Test
    void getDigitalDocument_NotFound() {
        // Arrange
        when(digitalDocumentRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AppException.class, () -> digitalDocumentService.getDigitalDocument(1L));
    }

    @Test
    void updateDigitalDocument_Success() throws IOException {
        // Arrange
        when(digitalDocumentRepository.findById(1L)).thenReturn(Optional.of(testDigitalDocument));
        when(documentRepository.save(any())).thenReturn(testDocument);
        when(digitalDocumentRepository.save(any())).thenReturn(testDigitalDocument);
        when(digitalDocumentMapper.toResponseDto(any())).thenReturn(testResponseDto);

        // Act
        DigitalDocumentResponseDto response = digitalDocumentService.updateDigitalDocument(1L, testRequestDto);

        // Assert
        assertNotNull(response);
        assertEquals(testResponseDto.getDigitalDocumentId(), response.getDigitalDocumentId());
        verify(documentRepository).save(any());
        verify(digitalDocumentRepository).save(any());
        verify(documentTextExtractionService).summarizeDoc(any(), anyBoolean());
    }

    @Test
    void deleteDigitalDocument_Success() {
        // Arrange
        when(digitalDocumentRepository.findById(1L)).thenReturn(Optional.of(testDigitalDocument));

        // Act
        digitalDocumentService.deleteDigitalDocument(1L);

        // Assert
        verify(digitalDocumentRepository).delete(testDigitalDocument);
    }

    @Test
    void updateVisibilityStatus_Success() {
        // Arrange
        testDocument.setApprovalStatus(ApprovalStatus.APPROVED);
        when(digitalDocumentRepository.findById(1L)).thenReturn(Optional.of(testDigitalDocument));
        when(digitalDocumentRepository.save(any())).thenReturn(testDigitalDocument);

        // Act
        digitalDocumentService.updateVisibilityStatus(1L, VisibilityStatus.PUBLIC);

        // Assert
        assertEquals(VisibilityStatus.PUBLIC, testDigitalDocument.getVisibilityStatus());
        verify(digitalDocumentRepository).save(testDigitalDocument);
    }

    @Test
    void updateVisibilityStatus_NotApproved() {
        // Arrange
        when(digitalDocumentRepository.findById(1L)).thenReturn(Optional.of(testDigitalDocument));

        // Act & Assert
        assertThrows(AppException.class, () -> 
            digitalDocumentService.updateVisibilityStatus(1L, VisibilityStatus.PUBLIC));
    }

    @Test
    void hasPermissionToAccess_PublicDocument() {
        // Arrange
        testDigitalDocument.setVisibilityStatus(VisibilityStatus.PUBLIC);
        when(digitalDocumentRepository.findById(1L)).thenReturn(Optional.of(testDigitalDocument));

        // Act
        boolean hasPermission = digitalDocumentService.hasPermissionToAccess(1L, testUser);

        // Assert
        assertTrue(hasPermission);
    }

    @Test
    void approveDigitalDocument_Success() {
        // Arrange
        when(digitalDocumentRepository.findById(1L)).thenReturn(Optional.of(testDigitalDocument));
        when(documentRepository.save(any())).thenReturn(testDocument);

        // Act
        digitalDocumentService.approveDigitalDocument(1L);

        // Assert
        assertEquals(ApprovalStatus.APPROVED, testDocument.getApprovalStatus());
        verify(documentRepository).save(testDocument);
        verify(documentTextExtractionService).summarizeDoc(any(), anyBoolean());
    }

    @Test
    void rejectDigitalDocument_Success() {
        // Arrange
        String rejectMessage = "Invalid content";
        when(digitalDocumentRepository.findById(1L)).thenReturn(Optional.of(testDigitalDocument));
        when(documentRepository.save(any())).thenReturn(testDocument);

        // Act
        digitalDocumentService.rejectDigitalDocument(1L, rejectMessage);

        // Assert
        assertEquals(ApprovalStatus.REJECTED, testDocument.getApprovalStatus());
        assertEquals(rejectMessage, testDocument.getReason_approval());
        verify(documentRepository).save(testDocument);
    }

    @Test
    void getDigitalDocumentsByUser_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        DocumentFilterRequest filter = new DocumentFilterRequest();
        List<DigitalDocumentEntity> documents = Collections.singletonList(testDigitalDocument);
        Page<DigitalDocumentEntity> documentPage = new PageImpl<>(documents, pageable, documents.size());

        when(userRepository.findById("test123")).thenReturn(Optional.of(testUser));
        when(roleService.isAdminOrManager(any())).thenReturn(true);
        when(digitalDocumentRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(documentPage);
        when(digitalDocumentMapper.toResponseDto(any())).thenReturn(testResponseDto);

        // Act
        Page<DigitalDocumentResponseDto> response = digitalDocumentService.getDigitalDocumentsByUser(pageable, "test123", filter);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        verify(digitalDocumentRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getPendingApprovalDocuments_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<DigitalDocumentEntity> documents = Collections.singletonList(testDigitalDocument);
        Page<DigitalDocumentEntity> documentPage = new PageImpl<>(documents, pageable, documents.size());

        when(digitalDocumentRepository.findByApprovalStatuses(any(), any())).thenReturn(documentPage);
        when(digitalDocumentMapper.toResponseDto(any())).thenReturn(testResponseDto);

        // Act
        Page<DigitalDocumentResponseDto> response = digitalDocumentService.getPendingApprovalDocuments(pageable);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        verify(digitalDocumentRepository).findByApprovalStatuses(any(), any());
    }
} 