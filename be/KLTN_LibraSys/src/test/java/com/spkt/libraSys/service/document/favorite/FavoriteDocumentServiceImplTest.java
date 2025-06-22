package com.spkt.libraSys.service.document.favorite;

import com.spkt.libraSys.service.access.AuthService;
import com.spkt.libraSys.service.document.DocumentEntity;
import com.spkt.libraSys.service.document.DocumentRepository;
import com.spkt.libraSys.service.user.UserEntity;
import com.spkt.libraSys.service.user.UserService;
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
class FavoriteDocumentServiceImplTest {

    @Mock
    private FavoriteDocumentRepository favoriteRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private UserService userService;
    @Mock
    private FavoriteDocumentMapper favoriteMapper;
    @Mock
    private AuthService authService;

    @InjectMocks
    private FavoriteDocumentServiceImpl favoriteDocumentService;

    private UserEntity testUser;
    private DocumentEntity testDocument;
    private FavoriteDocumentEntity testFavorite;
    private FavoriteDocumentResponseDto testResponseDto;

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
                .build();

        // Setup test favorite
        testFavorite = FavoriteDocumentEntity.builder()
                .favoriteId(1L)
                .user(testUser)
                .document(testDocument)
                .build();

        // Setup test response DTO
        testResponseDto = FavoriteDocumentResponseDto.builder()
                .documentId(1L)
                .documentName("Test Document")
                .build();
    }

    @Test
    void markFavorite_Success() {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
        when(favoriteRepository.existsByUserAndDocument(testUser, testDocument)).thenReturn(false);

        // Act
        favoriteDocumentService.markFavorite(1L);

        // Assert
        verify(favoriteRepository).save(any(FavoriteDocumentEntity.class));
    }

    @Test
    void markFavorite_AlreadyFavorite() {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
        when(favoriteRepository.existsByUserAndDocument(testUser, testDocument)).thenReturn(true);

        // Act
        favoriteDocumentService.markFavorite(1L);

        // Assert
        verify(favoriteRepository, never()).save(any(FavoriteDocumentEntity.class));
    }

    @Test
    void markFavorite_DocumentNotFound() {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(documentRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> favoriteDocumentService.markFavorite(999L));
    }

    @Test
    void unmarkFavorite_Success() {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));

        // Act
        favoriteDocumentService.unmarkFavorite(1L);

        // Assert
        verify(favoriteRepository).deleteByUserAndDocument(testUser, testDocument);
    }

    @Test
    void unmarkFavorite_DocumentNotFound() {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(documentRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> favoriteDocumentService.unmarkFavorite(999L));
    }

    @Test
    void getFavoriteDocuments_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<FavoriteDocumentEntity> favoritePage = new PageImpl<>(Collections.singletonList(testFavorite));
        Page<FavoriteDocumentResponseDto> expectedResponse = new PageImpl<>(Collections.singletonList(testResponseDto));

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(favoriteRepository.findAllByUser(testUser, pageable)).thenReturn(favoritePage);
        when(favoriteMapper.toDto(any(FavoriteDocumentEntity.class))).thenReturn(testResponseDto);

        // Act
        Page<FavoriteDocumentResponseDto> result = favoriteDocumentService.getFavoriteDocuments(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(testResponseDto, result.getContent().get(0));
    }

    @Test
    void isFavorite_True() {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
        when(favoriteRepository.existsByUserAndDocument(testUser, testDocument)).thenReturn(true);

        // Act
        boolean result = favoriteDocumentService.isFavorite(1L);

        // Assert
        assertTrue(result);
    }

    @Test
    void isFavorite_False() {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
        when(favoriteRepository.existsByUserAndDocument(testUser, testDocument)).thenReturn(false);

        // Act
        boolean result = favoriteDocumentService.isFavorite(1L);

        // Assert
        assertFalse(result);
    }

    @Test
    void isFavorite_DocumentNotFound() {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(documentRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> favoriteDocumentService.isFavorite(999L));
    }
} 