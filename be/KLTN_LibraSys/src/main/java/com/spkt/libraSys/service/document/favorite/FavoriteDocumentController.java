package com.spkt.libraSys.service.document.favorite;

import com.spkt.libraSys.exception.ApiResponse;
import com.spkt.libraSys.service.PageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class FavoriteDocumentController {

    private final FavoriteDocumentService favoriteService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageDTO<FavoriteDocumentResponseDto>>> getFavorites(Pageable pageable) {
        Page<FavoriteDocumentResponseDto> page = favoriteService.getFavoriteDocuments(pageable);
        return ResponseEntity.ok(
                ApiResponse.<PageDTO<FavoriteDocumentResponseDto>>builder()
                        .message("Danh sách tài liệu yêu thích")
                        .data(new PageDTO<>(page))
                        .build()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Boolean>> isFavorite(@PathVariable Long id) {
        boolean result = favoriteService.isFavorite(id);
        return ResponseEntity.ok(
                ApiResponse.<Boolean>builder()
                        .message("Trạng thái yêu thích đã được lấy")
                        .data(result)
                        .build()
        );
    }
}
