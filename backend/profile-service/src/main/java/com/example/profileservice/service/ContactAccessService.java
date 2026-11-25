package com.example.profileservice.service;

import com.example.profileservice.common.ApiResponse;
import com.example.profileservice.entity.ContactAccessLog;
import com.example.profileservice.entity.ContactRequest;
import com.example.profileservice.entity.CandidateProfile;
import com.example.profileservice.repository.CandidateProfileRepository;
import com.example.profileservice.repository.ContactAccessLogRepository;
import com.example.profileservice.repository.ContactRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactAccessService {

    private final ContactRequestRepository requestRepo;
    private final ContactAccessLogRepository logRepo;
    private final CandidateProfileRepository profileRepo;
    private final RestTemplate restTemplate;

    // Shared function - used by ALL APIs returning candidate info
    @Transactional(readOnly = true)
    public boolean canSeeContact(UUID companyId, UUID userId) {
        // Check 1: Candidate applied to this company's job
        if (hasApplication(companyId, userId)) {
            return true;
        }
        // Check 2: STAFF approved contact request
        return requestRepo.existsApprovedRequest(companyId, userId);
    }

    private boolean hasApplication(UUID companyId, UUID userId) {
        try {
            // Call recruitment-service to check if candidate applied to any job of this company
            // Endpoint: GET /api/v1/recruitment/applications/check?companyId={companyId}&userId={userId}
            String url = "http://recruitment-service:8086/api/v1/recruitment/applications/check"
                    + "?companyId=" + companyId + "&userId=" + userId;
            Boolean result = restTemplate.getForObject(url, Boolean.class);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.warn("Could not check application status: {}", e.getMessage());
            return false;
        }
    }

    @Transactional
    public ContactRequest requestContactAccess(UUID companyId, UUID hrId, UUID userId, UUID jobId, String reason) {
        // Check if already has pending or approved request
        Optional<ContactRequest> existing = requestRepo.findByCompanyIdAndUserIdAndJobId(companyId, userId, jobId);
        if (existing.isPresent()) {
            ContactRequest cr = existing.get();
            if (cr.getStatus() == ContactRequest.ContactRequestStatus.PENDING) {
                throw new IllegalStateException("Yêu cầu mở liên hệ đang chờ duyệt");
            }
            if (cr.getStatus() == ContactRequest.ContactRequestStatus.APPROVED) {
                throw new IllegalStateException("Đã được duyệt mở liên hệ");
            }
            // If REJECTED, allow new request
        }

        ContactRequest request = ContactRequest.builder()
                .companyId(companyId)
                .userId(userId)
                .jobId(jobId)
                .reason(reason)
                .status(ContactRequest.ContactRequestStatus.PENDING)
                .build();
        return requestRepo.save(request);
    }

    @Transactional
    public ContactRequest approveRequest(UUID requestId, UUID staffId) {
        ContactRequest request = requestRepo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu"));
        if (request.getStatus() != ContactRequest.ContactRequestStatus.PENDING) {
            throw new IllegalStateException("Chỉ duyệt được yêu cầu đang chờ");
        }
        request.setStatus(ContactRequest.ContactRequestStatus.APPROVED);
        request.setReviewedBy(staffId);
        request.setReviewedAt(OffsetDateTime.now());
        return requestRepo.save(request);
    }

    @Transactional
    public ContactRequest rejectRequest(UUID requestId, UUID staffId, String reason) {
        ContactRequest request = requestRepo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu"));
        if (request.getStatus() != ContactRequest.ContactRequestStatus.PENDING) {
            throw new IllegalStateException("Chỉ từ chối được yêu cầu đang chờ");
        }
        request.setStatus(ContactRequest.ContactRequestStatus.REJECTED);
        request.setReviewedBy(staffId);
        request.setReviewedAt(OffsetDateTime.now());
        // Could store rejection reason in a separate field or log
        return requestRepo.save(request);
    }

    @Transactional(readOnly = true)
    public ContactResponse getContact(UUID companyId, UUID hrId, UUID userId) {
        if (!canSeeContact(companyId, userId)) {
            throw new ContactAccessDeniedException("CONTACT_LOCKED");
        }

        CandidateProfile profile = profileRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ứng viên"));

        // Log access
        ContactAccessLog.ViaType via = hasApplication(companyId, userId)
                ? ContactAccessLog.ViaType.APPLICATION
                : ContactAccessLog.ViaType.APPROVED_REQUEST;
        logAccess(companyId, hrId, userId, via);

        return ContactResponse.builder()
                .email(profile.getEmail())
                .phone(profile.getPhone())
                .build();
    }

    @Transactional
    public void logAccess(UUID companyId, UUID hrId, UUID userId, ContactAccessLog.ViaType via) {
        ContactAccessLog log = ContactAccessLog.builder()
                .companyId(companyId)
                .hrId(hrId)
                .userId(userId)
                .via(via)
                .build();
        logRepo.save(log);
    }

    @Transactional(readOnly = true)
    public List<ContactAccessLog> getAccessLogs(UUID companyId, UUID userId, OffsetDateTime from, OffsetDateTime to) {
        if (from != null && to != null) {
            return logRepo.findByDateRange(from, to).stream()
                    .filter(l -> companyId == null || l.getCompanyId().equals(companyId))
                    .filter(l -> userId == null || l.getUserId().equals(userId))
                    .toList();
        }
        if (companyId != null && userId != null) {
            return logRepo.findByCompanyIdAndUserId(companyId, userId);
        }
        if (companyId != null) {
            return logRepo.findByCompanyId(companyId);
        }
        if (userId != null) {
            return logRepo.findByUserId(userId);
        }
        return logRepo.findAll();
    }

    @Transactional(readOnly = true)
    public List<ContactAccessLog> getMyAccessLogs(UUID userId) {
        return logRepo.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<ContactRequest> getPendingRequestsForUser(UUID userId) {
        return requestRepo.findPendingByUserId(userId);
    }

    public List<ContactRequest> getAllRequests() {
        return requestRepo.findAll();
    }

    public List<ContactRequest> getRequestsByStatus(ContactRequest.ContactRequestStatus status) {
        return requestRepo.findByStatus(status);
    }

    // DTO
    @lombok.Value @lombok.Builder
    public static class ContactResponse {
        String email;
        String phone;
    }

    public static class ContactAccessDeniedException extends RuntimeException {
        public ContactAccessDeniedException(String message) { super(message); }
    }
}