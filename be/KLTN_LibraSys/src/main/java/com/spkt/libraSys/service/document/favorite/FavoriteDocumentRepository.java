package com.spkt.libraSys.service.document.favorite;

import com.spkt.libraSys.service.document.DocumentEntity;
import com.spkt.libraSys.service.user.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FavoriteDocumentRepository extends JpaRepository<FavoriteDocumentEntity, Long> {
    Page<FavoriteDocumentEntity> findAllByUser(UserEntity user, Pageable pageable);
    boolean existsByUserAndDocument(UserEntity user, DocumentEntity document);
    void deleteByUserAndDocument(UserEntity user, DocumentEntity document);
    List<FavoriteDocumentEntity> findByUser(UserEntity user);

    long countByUser(UserEntity user);
}
