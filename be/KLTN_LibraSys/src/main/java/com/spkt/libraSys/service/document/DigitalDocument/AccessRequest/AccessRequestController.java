package com.spkt.libraSys.service.document.DigitalDocument.AccessRequest;

import com.spkt.libraSys.exception.ApiResponse;
import com.spkt.libraSys.service.PageDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/access-requests")
@RequiredArgsConstructor
public class AccessRequestController {

    private final AccessRequestService accessRequestService;

    @PostMapping
    public ResponseEntity<ApiResponse<AccessRequestResponseDto>> sendRequest(@Valid @RequestBody AccessRequestDto dto) throws Exception {
        AccessRequestResponseDto responseDto = accessRequestService.sendRequest(dto.getDigitalId());

        ApiResponse<AccessRequestResponseDto> apiResponse = ApiResponse.<AccessRequestResponseDto>builder()
                .message("Gửi yêu cầu truy cập thành công")
                .data(responseDto)
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<AccessRequestResponseDto>> approve(@PathVariable Long id) throws Exception {
        AccessRequestResponseDto responseDto = accessRequestService.approveRequest(id);

        ApiResponse<AccessRequestResponseDto> apiResponse = ApiResponse.<AccessRequestResponseDto>builder()
                .message("Yêu cầu đã được phê duyệt")
                .data(responseDto)
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<AccessRequestResponseDto>> reject(@PathVariable Long id) throws Exception {
        AccessRequestResponseDto responseDto = accessRequestService.rejectRequest(id);

        ApiResponse<AccessRequestResponseDto> apiResponse = ApiResponse.<AccessRequestResponseDto>builder()
                .message("Yêu cầu đã bị từ chối")
                .data(responseDto)
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageDTO<AccessRequestResponseDto>>> getAllAccessRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String sort) {

        String[] sortParams = sort.split(",");
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.by(sortParams[0]).with(Sort.Direction.fromString(sortParams[1]))));

        PageDTO<AccessRequestResponseDto> pageResponse = accessRequestService.getAllRequests(pageable);

        ApiResponse<PageDTO<AccessRequestResponseDto>> apiResponse = ApiResponse.<PageDTO<AccessRequestResponseDto>>builder()
                .message("Danh sách yêu cầu truy cập")
                .data(pageResponse)
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AccessRequestResponseDto>> getAccessRequestById(@PathVariable Long id) throws Exception {
        AccessRequestResponseDto responseDto = accessRequestService.getRequestById(id);

        ApiResponse<AccessRequestResponseDto> apiResponse = ApiResponse.<AccessRequestResponseDto>builder()
                .message("Chi tiết yêu cầu truy cập")
                .data(responseDto)
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<PageDTO<AccessRequestResponseDto>>> getAccessRequestsByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String sort) throws Exception {

        String[] sortParams = sort.split(",");
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.by(sortParams[0]).with(Sort.Direction.fromString(sortParams[1]))));

        PageDTO<AccessRequestResponseDto> pageResponse = accessRequestService.getRequestsByStatus(status, pageable);

        ApiResponse<PageDTO<AccessRequestResponseDto>> apiResponse = ApiResponse.<PageDTO<AccessRequestResponseDto>>builder()
                .message("Danh sách yêu cầu truy cập theo trạng thái")
                .data(pageResponse)
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/user")
    public ResponseEntity<ApiResponse<PageDTO<AccessRequestResponseDto>>> getAccessRequestsByUser(
            @RequestParam String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String sort) throws Exception {

        String[] sortParams = sort.split(",");
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.by(sortParams[0]).with(Sort.Direction.fromString(sortParams[1]))));

        PageDTO<AccessRequestResponseDto> pageResponse = accessRequestService.getRequestsByUser(userId, pageable);

        ApiResponse<PageDTO<AccessRequestResponseDto>> apiResponse = ApiResponse.<PageDTO<AccessRequestResponseDto>>builder()
                .message("Danh sách yêu cầu truy cập của người dùng")
                .data(pageResponse)
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> cancelAccessRequest(@PathVariable Long id) throws Exception {
        accessRequestService.cancelRequest(id);

        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .message("Yêu cầu truy cập đã được hủy")
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AccessRequestResponseDto>> updateAccessRequest(
            @PathVariable Long id,
            @Valid @RequestBody AccessRequestDto dto) throws Exception {
        AccessRequestResponseDto responseDto = accessRequestService.updateRequest(id, dto);

        ApiResponse<AccessRequestResponseDto> apiResponse = ApiResponse.<AccessRequestResponseDto>builder()
                .message("Yêu cầu truy cập đã được cập nhật")
                .data(responseDto)
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/digital/{digitalId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBorrowersByDigitalId(
            @PathVariable Long digitalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String sort) throws Exception {

        String[] sortParams = sort.split(",");
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.by(sortParams[0]).with(Sort.Direction.fromString(sortParams[1]))));

        Map<String, Object> response = accessRequestService.getBorrowersByDigitalId(digitalId, pageable);

        ApiResponse<Map<String, Object>> apiResponse = ApiResponse.<Map<String, Object>>builder()
                .message("Danh sách người mượn theo tài liệu")
                .data(response)
                .build();

        return ResponseEntity.ok(apiResponse);
    }
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @GetMapping("/digital")
    public ResponseEntity<ApiResponse<List<AccessRequestResponseDto>>> getBorrowers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String sort) throws Exception {

        String[] sortParams = sort.split(",");
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.by(sortParams[0]).with(Sort.Direction.fromString(sortParams[1]))));

        List<AccessRequestResponseDto> response = accessRequestService.getBorrowers( pageable);

        ApiResponse<List<AccessRequestResponseDto>> apiResponse = ApiResponse.<List<AccessRequestResponseDto>>builder()
                .message("Danh sách người mượn theo tài liệu")
                .data(response)
                .build();

        return ResponseEntity.ok(apiResponse);
    }
    

}
