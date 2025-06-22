package com.spkt.libraSys.service.document.programClass;


import com.spkt.libraSys.exception.ApiResponse;
import com.spkt.libraSys.service.PageDTO;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/program-classes")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProgramClassController {

    ProgramClassService programClassService;

    @PostMapping("/upload")
    @RateLimiter(name = "programClassUploadLimiter")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<ProgramClassUploadResult>> uploadProgramClasses(@RequestParam("file") MultipartFile file) {
        log.info("📥 Upload chương trình học từ file Excel");

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.<ProgramClassUploadResult>builder()
                    .code(400)
                    .message("❌ File không được để trống")
                    .build());
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
            return ResponseEntity.badRequest().body(ApiResponse.<ProgramClassUploadResult>builder()
                    .code(400)
                    .message("❌ Chỉ hỗ trợ file Excel (.xlsx, .xls)")
                    .build());
        }

        ProgramClassUploadResult result = programClassService.saveProgramClassesFromExcel(file);
        return ResponseEntity.ok(ApiResponse.<ProgramClassUploadResult>builder()
                .message("✅ Đã xử lý file Excel thành công")
                .data(result)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProgramClassResponse>> getProgramClassById(@PathVariable Long id) {
        ProgramClassResponse response = programClassService.getProgramClassById(id);
        return ResponseEntity.ok(ApiResponse.<ProgramClassResponse>builder()
                .message("✅ Lấy thông tin chương trình học thành công")
                .data(response)
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageDTO<ProgramClassResponse>>> getAllProgramClasses(Pageable pageable) {
        Page<ProgramClassResponse> page = programClassService.getAllProgramClasses(pageable);
        return ResponseEntity.ok(ApiResponse.<PageDTO<ProgramClassResponse>>builder()
                .message("✅ Danh sách chương trình học")
                .data(new PageDTO<>(page))
                .build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<ProgramClassResponse>> createProgramClass(
            @Valid @RequestBody ProgramClassResponse request) {
        ProgramClassResponse response = programClassService.createProgramClass(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<ProgramClassResponse>builder()
                .message("✅ Tạo chương trình học thành công")
                .data(response)
                .build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<ProgramClassResponse>> updateProgramClass(
            @PathVariable Long id,
            @Valid @RequestBody ProgramClassResponse request) {
        ProgramClassResponse response = programClassService.updateProgramClass(id, request);
        return ResponseEntity.ok(ApiResponse.<ProgramClassResponse>builder()
                .message("✅ Cập nhật chương trình học thành công")
                .data(response)
                .build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteProgramClass(@PathVariable Long id) {
        programClassService.deleteProgramClass(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("✅ Đã xóa chương trình học")
                .build());
    }

    @DeleteMapping("/batch")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteProgramClasses(@RequestBody List<Long> ids) {
        programClassService.deleteProgramClasses(ids);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("✅ Đã xóa các chương trình học được chọn")
                .build());
    }
}
