package com.spkt.libraSys.service.document.DigitalDocument.AccessRequest;

import com.spkt.libraSys.service.PageDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface AccessRequestService {
    AccessRequestResponseDto sendRequest(Long digitalId) throws Exception;
    AccessRequestResponseDto approveRequest(Long requestId) throws Exception;
    AccessRequestResponseDto rejectRequest(Long requestId) throws Exception;
    PageDTO<AccessRequestResponseDto> getAllRequests(Pageable pageable);
    
    // New methods
    AccessRequestResponseDto getRequestById(Long id) throws Exception;
    PageDTO<AccessRequestResponseDto> getRequestsByStatus(String status, Pageable pageable) throws Exception;
    PageDTO<AccessRequestResponseDto> getRequestsByUser(String userId, Pageable pageable) throws Exception;
    void cancelRequest(Long id) throws Exception;
    AccessRequestResponseDto updateRequest(Long id, AccessRequestDto dto) throws Exception;
    Map<String, Object> getBorrowersByDigitalId(Long digitalId, Pageable pageable) throws Exception;
    List<AccessRequestResponseDto> getBorrowers(Pageable pageable) throws Exception;
}
