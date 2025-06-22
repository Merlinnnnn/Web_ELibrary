package com.spkt.libraSys.service.document.DigitalDocument.AccessRequest;

import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;
import com.spkt.libraSys.service.PageDTO;
import com.spkt.libraSys.service.access.AuthService;
import com.spkt.libraSys.service.document.DigitalDocument.DigitalDocumentEntity;
import com.spkt.libraSys.service.document.DigitalDocument.DigitalDocumentRepository;
import com.spkt.libraSys.service.document.DigitalDocument.VisibilityStatus;
import com.spkt.libraSys.service.document.DocumentEntity;
import com.spkt.libraSys.service.document.upload.UploadEntity;
import com.spkt.libraSys.service.document.upload.UploadRepository;
import com.spkt.libraSys.service.document.viewer.ApprovalStatus;
import com.spkt.libraSys.service.role.RoleService;
import com.spkt.libraSys.service.user.UserEntity;
import com.spkt.libraSys.service.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AccessRequestServiceImpl implements AccessRequestService {

    @Autowired
    private AccessRequestRepository accessRequestRepository;
    @Autowired
    private UploadRepository uploadRepository;
    @Autowired
    private DigitalDocumentRepository digitalDocumentRepository;
    @Autowired
    private AuthService authService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private AccessRequestMapper accessRequestMapper;
    @Autowired
    private UserRepository userRepository;

    private final  int ACCESS_DAYS_APPROVE = 30;

    @Override
    public AccessRequestResponseDto sendRequest(Long digitalId) throws Exception {
        UserEntity userCurr = authService.getCurrentUser();
        String requesterId = userCurr.getUserId();
        DigitalDocumentEntity digitalDocument = digitalDocumentRepository.findById(digitalId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy tài liệu"));
        DocumentEntity document = digitalDocument.getDocument();
        if(!document.getApprovalStatus().equals(ApprovalStatus.APPROVED)){
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        String userOwnerId = digitalDocument.getUser().getUserId();
        List<AccessRequestEntity> existingRequests =
                accessRequestRepository.findByDigitalIdAndRequesterId(digitalId, requesterId);

        for (AccessRequestEntity req : existingRequests) {
            LocalDateTime licenseExpiry = req.getLicenseExpiry();
            if (licenseExpiry == null || licenseExpiry.isAfter(LocalDateTime.now())) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Quyền truy cập vẫn còn hiệu lực, không thể gửi lại yêu cầu");
            }
        }
        if (requesterId.equals(userOwnerId)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Không thể gửi yêu cầu truy cập tài liệu của chính bạn");
        }

        AccessRequestStatus status = digitalDocument.getVisibilityStatus()
                .equals(VisibilityStatus.PUBLIC)?  AccessRequestStatus.APPROVED: AccessRequestStatus.PENDING;


        AccessRequestEntity request = new AccessRequestEntity();
        request.setDigitalId(digitalId);
        request.setRequesterId(requesterId);
        request.setOwnerId(userOwnerId);
        request.setStatus(status);
        request.setRequestTime(LocalDateTime.now());
        request.setCoverImage(digitalDocument.getDocument().getCoverImage());

        AccessRequestEntity savedRequest = accessRequestRepository.save(request);

        return covertToDto(savedRequest);
    }

    @Override
    public AccessRequestResponseDto approveRequest(Long requestId) throws Exception {
        UserEntity currentUser = authService.getCurrentUser();

        AccessRequestEntity request = accessRequestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Request not found"));

        if (!canApproveOrReject(request, currentUser)) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "You do not have permission to approve this request");
        }

        request.setStatus(AccessRequestStatus.APPROVED);
        request.setDecisionTime(LocalDateTime.now());
        request.setLicenseExpiry(LocalDateTime.now().plusDays(ACCESS_DAYS_APPROVE));  // 30 days after approval
        request.setReviewerId(currentUser.getUserId());
        AccessRequestEntity savedRequest = accessRequestRepository.save(request);

        return covertToDto(savedRequest);
    }

    @Override
    public AccessRequestResponseDto rejectRequest(Long requestId) throws Exception {
        UserEntity currentUser = authService.getCurrentUser();
        AccessRequestEntity request = accessRequestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Request not found"));

        if (!canApproveOrReject(request, currentUser)) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "You do not have permission to reject this request");
        }

        request.setStatus(AccessRequestStatus.REJECTED);
        request.setDecisionTime(LocalDateTime.now());
        request.setReviewerId(currentUser.getUserId());
        AccessRequestEntity savedRequest = accessRequestRepository.save(request);

        return covertToDto(savedRequest);
    }

    @Override
    public PageDTO<AccessRequestResponseDto> getAllRequests(Pageable pageable) {
        UserEntity currentUser = authService.getCurrentUser();

        if (roleService.isAdminOrManager(currentUser)) {
            Page<AccessRequestEntity> getAll = accessRequestRepository.findAll(pageable);
            return new PageDTO<>(getAll.map(accessRequestMapper::entityToDto));
        } else {
            Page<AccessRequestEntity> userRequests = accessRequestRepository.findByOwnerId(currentUser.getUserId(), pageable);
            return new PageDTO<>(userRequests.map(accessRequestMapper::entityToDto));
        }
    }

    @Override
    public AccessRequestResponseDto getRequestById(Long id) throws Exception {
        UserEntity currentUser = authService.getCurrentUser();
        AccessRequestEntity request = accessRequestRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Request not found"));

        // Check if user has permission to view this request
        if (!canViewRequest(request, currentUser)) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "You do not have permission to view this request");
        }

        return covertToDto(request);
    }

    @Override
    public PageDTO<AccessRequestResponseDto> getRequestsByStatus(String status, Pageable pageable) throws Exception {
        UserEntity currentUser = authService.getCurrentUser();
        AccessRequestStatus requestStatus = AccessRequestStatus.valueOf(status.toUpperCase());

        Page<AccessRequestEntity> requests;
        if (roleService.isAdminOrManager(currentUser)) {
            requests = accessRequestRepository.findByStatus(requestStatus, pageable);
        } else {
            requests = accessRequestRepository.findByStatusAndOwnerId(requestStatus, currentUser.getUserId(), pageable);
        }

        return new PageDTO<>(requests.map(accessRequestMapper::entityToDto));
    }

    @Override
    public PageDTO<AccessRequestResponseDto> getRequestsByUser(String userId, Pageable pageable) throws Exception {
        UserEntity currentUser = authService.getCurrentUser();

        // Check if user has permission to view other user's requests
        if (!userId.equals(currentUser.getUserId()) && !roleService.isAdminOrManager(currentUser)) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "You do not have permission to view other user's requests");
        }

        Page<AccessRequestEntity> requests = accessRequestRepository.findByRequesterId(userId, pageable);
        return new PageDTO<>(requests.map(accessRequestMapper::entityToDto));
    }

    @Override
    public void cancelRequest(Long id) throws Exception {
        UserEntity currentUser = authService.getCurrentUser();
        AccessRequestEntity request = accessRequestRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Request not found"));

        // Only requester can cancel their own request
        if (!request.getRequesterId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "You can only cancel your own requests");
        }

        // Can only cancel pending requests
        if (request.getStatus() != AccessRequestStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Can only cancel pending requests");
        }

        accessRequestRepository.delete(request);
    }

    @Override
    public AccessRequestResponseDto updateRequest(Long id, AccessRequestDto dto) throws Exception {
        UserEntity currentUser = authService.getCurrentUser();
        AccessRequestEntity request = accessRequestRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Request not found"));

        // Only requester can update their own request
        if (!request.getRequesterId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "You can only update your own requests");
        }

        // Can only update pending requests
        if (request.getStatus() != AccessRequestStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Can only update pending requests");
        }

        // Update request fields
        if (dto.getDigitalId() != null) {
            DigitalDocumentEntity digitalDocument = digitalDocumentRepository.findById(dto.getDigitalId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Document not found"));
            request.setDigitalId(dto.getDigitalId());
            request.setOwnerId(digitalDocument.getUser().getUserId());
        }

        AccessRequestEntity updatedRequest = accessRequestRepository.save(request);
        return covertToDto(updatedRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getBorrowersByDigitalId(Long digitalId, Pageable pageable) throws Exception {
        UserEntity currentUser = authService.getCurrentUser();
        
        // Kiểm tra quyền truy cập
        DigitalDocumentEntity digitalDocument = digitalDocumentRepository.findById(digitalId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Document not found"));
                
        if (!digitalDocument.getUser().getUserId().equals(currentUser.getUserId()) 
            && !roleService.isAdminOrManager(currentUser)) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "You do not have permission to view this document's borrowers");
        }

        Map<String, Object> response = new HashMap<>();
        
        // Lấy danh sách yêu cầu truy cập theo digitalId
        Page<AccessRequestEntity> requests = accessRequestRepository.findByDigitalId(digitalId, pageable);
        
        // Chuyển đổi sang DTO
        List<AccessRequestResponseDto> borrowers = requests.getContent().stream()
                .map(this::covertToDto)
                .collect(Collectors.toList());
        
        // Thêm thông tin vào response
        response.put("digitalId", digitalId);
        response.put("totalBorrowers", requests.getTotalElements());
        response.put("borrowers", borrowers);
        
        return response;
    }

    @Override
    public List<AccessRequestResponseDto> getBorrowers(Pageable pageable) throws Exception {
        List<AccessRequestResponseDto> result = new ArrayList<>();

        // Fetch all digital documents
        List<DigitalDocumentEntity> digitalDocuments = digitalDocumentRepository.findAll();

        for (DigitalDocumentEntity digitalDocument : digitalDocuments) {
            // Skip if uploader is not admin or manager
            if (!roleService.isAdminOrManager(digitalDocument.getUser())) {
                continue;
            }

            Long digitalId = digitalDocument.getDigitalDocumentId();

            // Fetch paginated access requests for this document
            Page<AccessRequestEntity> requests = accessRequestRepository.findByDigitalId(digitalId, pageable);

            // Convert to DTO and add to result list
            requests.getContent().stream()
                    .map(this::covertToDto)
                    .forEach(result::add);
        }

        return result;
    }



    private boolean canApproveOrReject(AccessRequestEntity request, UserEntity currentUser) {
        String ownerId = request.getOwnerId();

        if (ownerId.equals(currentUser.getUserId())) {
            return true;
        }
        UserEntity userOwner = userRepository.findById(ownerId).orElse(null);

        if (userOwner == null) {
            return roleService.isAdminOrManager(currentUser);
        }
        if (roleService.hasRole(ownerId, "MANAGER")) {
            return roleService.isAdminOrManager(currentUser);
        }
        return false;
    }


    private boolean canViewRequest(AccessRequestEntity request, UserEntity currentUser) {
        return request.getRequesterId().equals(currentUser.getUserId()) ||
               request.getOwnerId().equals(currentUser.getUserId()) ||
               roleService.isAdminOrManager(currentUser);
    }

    private AccessRequestResponseDto covertToDto(AccessRequestEntity request) {
        AccessRequestResponseDto dto  = accessRequestMapper.entityToDto(request);
        Optional<UserEntity> currentRequest = userRepository.findById(dto.getRequesterId());
        Optional<UserEntity> currentOwner = userRepository.findById(dto.getOwnerId());
        currentRequest.ifPresent(userEntity -> dto.setRequesterName(userEntity.getUsername()));
        currentOwner.ifPresent(userEntity -> dto.setOwnerName(userEntity.getUsername()));
        return dto;


    }
}
