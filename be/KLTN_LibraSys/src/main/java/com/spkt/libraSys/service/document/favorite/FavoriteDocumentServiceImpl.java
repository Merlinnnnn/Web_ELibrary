package com.spkt.libraSys.service.document.favorite;

import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;
import com.spkt.libraSys.service.access.AuthService;
import com.spkt.libraSys.service.document.DocumentEntity;
import com.spkt.libraSys.service.document.DocumentRepository;
import com.spkt.libraSys.service.user.UserEntity;
import com.spkt.libraSys.service.user.UserService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FavoriteDocumentServiceImpl implements FavoriteDocumentService {

    FavoriteDocumentRepository favoriteRepository;
    DocumentRepository documentRepository;
    UserService userService;
    FavoriteDocumentMapper favoriteMapper;
    AuthService authService;


    @Override
    public void markFavorite(Long documentId) {
        UserEntity user = authService.getCurrentUser();
        DocumentEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        if (!favoriteRepository.existsByUserAndDocument(user, document)) {
            FavoriteDocumentEntity favorite = FavoriteDocumentEntity.builder()
                    .user(user)
                    .document(document)
                    .build();
            favoriteRepository.save(favorite);
        }
    }

    @Override
    @Transactional
    public void unmarkFavorite(Long documentId) {
        UserEntity user = authService.getCurrentUser();
        DocumentEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        favoriteRepository.deleteByUserAndDocument(user, document);
    }

    @Override
    public Page<FavoriteDocumentResponseDto> getFavoriteDocuments(Pageable pageable) {
        UserEntity user = authService.getCurrentUser();

        return favoriteRepository.findAllByUser(user, pageable)
                .map(favoriteMapper::toDto);
    }

    @Override
    public boolean isFavorite(Long documentId) {
        UserEntity user = authService.getCurrentUser();
        DocumentEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        return favoriteRepository.existsByUserAndDocument(user, document);
    }
}
