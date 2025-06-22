package com.spkt.libraSys.service.recommendation;


import com.spkt.libraSys.exception.ApiResponse;
import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;
import com.spkt.libraSys.service.PageDTO;
import com.spkt.libraSys.service.document.DocumentResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import com.spkt.libraSys.service.user.UserRepository;
import com.spkt.libraSys.service.user.UserEntity;
import org.springframework.http.ResponseEntity;
import java.util.List;
import com.spkt.libraSys.service.access.AuthService;

/**
 * Controller providing APIs for document recommendations to users.
 * Handles both program-based and ML-based recommendation endpoints.
 */
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final UserRepository userRepository;
    private final MLRecommendationService mlRecommendationService;
    private final AuthService authService;

    /**
     * Retrieves a list of recommended documents for the current user with pagination support.
     *
     * @param page Page number (starting from 0), defaults to 0
     * @param size Number of items per page, defaults to 10
     * @return ApiResponse containing the list of recommended documents
     */
    @GetMapping("/program")
    public ApiResponse<PageDTO<DocumentResponseDto>> getUserDocumentRecommendations(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);

        PageDTO<DocumentResponseDto> recommendations =
                recommendationService.getRecommendedDocumentsForCurrentUser(pageable);

        return ApiResponse.<PageDTO<DocumentResponseDto>>builder()
                .data(recommendations)
                .message("✅ Lấy đề xuất tài liệu thành công.")
                .build();
    }

    /**
     * Retrieves ML-based document recommendations with pagination support.
     *
     * @param page Page number (starting from 0), defaults to 0
     * @param size Number of items per page, defaults to 10
     * @return ResponseEntity containing ApiResponse with ML-based recommendations
     */
    @GetMapping("/ml")
    public ResponseEntity<ApiResponse<PageDTO<DocumentResponseDto>>> getRecommendations(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PageDTO<DocumentResponseDto> recommendations = mlRecommendationService.getMLRecommendations(pageable);
        return ResponseEntity.ok(ApiResponse.<PageDTO<DocumentResponseDto>>builder()
                .data(recommendations).build());
    }
}
