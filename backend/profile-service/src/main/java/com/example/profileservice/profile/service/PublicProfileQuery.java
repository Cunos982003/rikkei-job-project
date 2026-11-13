package com.example.profileservice.profile.service;

import com.example.profileservice.profile.domain.CandidateProfile;
import com.example.profileservice.profile.domain.CandidateProfile.Visibility;
import com.example.profileservice.profile.infrastructure.CandidateProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Điều kiện dùng chung "chỉ hồ sơ PUBLIC".
 *
 * <p>Doanh nghiệp chỉ được thấy hồ sơ khi ứng viên đã đồng ý công khai và cán bộ đã duyệt.
 * Mọi truy vấn hướng doanh nghiệp (DN-03 tìm ứng viên, DN-06 chi tiết ứng viên, UT-03 kiểm thử)
 * phải đi qua đây thay vì tự viết lại điều kiện, để trạng thái PENDING/REJECTED/PRIVATE
 * không rò rỉ ở bất kỳ đường truy vấn nào.
 */
@Component
@RequiredArgsConstructor
public class PublicProfileQuery {

    private final CandidateProfileRepository profileRepo;

    public List<CandidateProfile> findAll() {
        return profileRepo.findByVisibility(Visibility.PUBLIC);
    }

    public Page<CandidateProfile> findAll(Pageable pageable) {
        return profileRepo.findByVisibility(Visibility.PUBLIC, pageable);
    }

    /**
     * Tra một hồ sơ theo userId, chỉ trả về khi hồ sơ đang PUBLIC.
     */
    public Optional<CandidateProfile> findById(UUID userId) {
        return profileRepo.findByUserId(userId)
                .filter(profile -> profile.getVisibility() == Visibility.PUBLIC);
    }

    public boolean isVisibleToRecruiters(UUID userId) {
        return findById(userId).isPresent();
    }
}
