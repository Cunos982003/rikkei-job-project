package com.example.profileservice.service;

import com.example.profileservice.dto.PortfolioProjectDto;
import com.example.profileservice.entity.PortfolioMedia;
import com.example.profileservice.entity.PortfolioProject;
import com.example.profileservice.exception.InvalidFileException;
import com.example.profileservice.exception.ResourceNotFoundException;
import com.example.profileservice.repository.PortfolioMediaRepository;
import com.example.profileservice.repository.PortfolioProjectRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioProjectRepository projectRepository;
    private final PortfolioMediaRepository mediaRepository;
    private final MinioFileUploadService minioFileUploadService;

    public static final int MAX_IMAGES_PER_PROJECT = 6;

    @Transactional(readOnly = true)
    public List<PortfolioProjectDto> getMyPortfolio(UUID userId) {
        List<PortfolioProject> projects = projectRepository.findByUserIdOrderBySortOrderAsc(userId);
        return projects.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PortfolioProjectDto> getPublicPortfolio(UUID userId) {
        List<PortfolioProject> projects = projectRepository.findByUserIdAndVisibleTrueOrderBySortOrderAsc(userId);
        return projects.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public PortfolioProjectDto createProject(UUID userId, @Valid PortfolioProjectDto dto) {
        // Check if this is an IOC project (from internship sync)
        if (dto.getInternshipId() != null) {
            // IOC projects are created/updated only by sync, not manually
            throw new IllegalStateException("Dự án thực tập (IOC) không thể tạo thủ công, chỉ đồng bộ từ PM");
        }

        PortfolioProject project = new PortfolioProject();
        project.setUserId(userId);
        project.setSource("MANUAL");
        project.setSourceRef(UUID.randomUUID().toString());
        updateProjectFromDto(project, dto);

        // Set sort_order to next available
        int maxSortOrder = projectRepository.findByUserIdOrderBySortOrderAsc(userId)
                .stream().mapToInt(PortfolioProject::getSortOrder).max().orElse(-1);
        project.setSortOrder(maxSortOrder + 1);

        project = projectRepository.save(project);
        return toDto(project);
    }

    @Transactional
    public PortfolioProjectDto updateProject(UUID userId, UUID projectId, @Valid PortfolioProjectDto dto) {
        PortfolioProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dự án portfolio"));

        if (!project.getUserId().equals(userId)) {
            throw new SecurityException("Không có quyền truy cập dự án này");
        }

        // IOC project protection: cannot delete, limited edits
        if (project.getSource() != null && project.getSource().equals("IOC")) {
            // Cannot change source, source_ref, internship_id
            // Can only update: name, role, description, tech_stack, repo_url, demo_url,
            // start_date, end_date, visible, sort_order
            updateIocProjectFields(project, dto);
        } else {
            updateProjectFromDto(project, dto);
        }

        project = projectRepository.save(project);
        return toDto(project);
    }

    @Transactional
    public void deleteProject(UUID userId, UUID projectId) {
        PortfolioProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dự án portfolio"));

        if (!project.getUserId().equals(userId)) {
            throw new SecurityException("Không có quyền truy cập dự án này");
        }

        // IOC project protection: cannot delete
        if (project.getSource() != null && project.getSource().equals("IOC")) {
            throw new IllegalStateException("Không thể xóa dự án thực tập (IOC)");
        }

        // Delete associated media files from MinIO
        List<PortfolioMedia> mediaList = mediaRepository.findByProjectIdOrderBySortOrderAsc(projectId);
        for (PortfolioMedia media : mediaList) {
            try {
                minioFileUploadService.delete(media.getUrl());
            } catch (Exception ignored) {
                // Log but continue deletion
            }
        }

        mediaRepository.deleteByProjectId(projectId);
        projectRepository.delete(project);
    }

    @Transactional
    public List<PortfolioProjectDto> reorderProjects(UUID userId, List<UUID> projectIds) {
        // Verify all projects belong to user
        List<PortfolioProject> projects = projectRepository.findByUserIdOrderBySortOrderAsc(userId);
        List<UUID> userProjectIds = projects.stream().map(PortfolioProject::getId).collect(Collectors.toList());

        if (!userProjectIds.containsAll(projectIds) || projectIds.size() != userProjectIds.size()) {
            throw new IllegalArgumentException("Danh sách dự án không hợp lệ");
        }

        for (int i = 0; i < projectIds.size(); i++) {
            UUID id = projectIds.get(i);
            PortfolioProject project = projects.stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null);
            if (project != null) {
                project.setSortOrder(i);
                projectRepository.save(project);
            }
        }

        return getMyPortfolio(userId);
    }

    @Transactional
    public PortfolioProjectDto.PortfolioMediaDto uploadMedia(UUID userId, UUID projectId, MultipartFile file) {
        PortfolioProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dự án portfolio"));

        if (!project.getUserId().equals(userId)) {
            throw new SecurityException("Không có quyền truy cập dự án này");
        }

        // Check max images limit
        long currentCount = mediaRepository.findByProjectIdOrderBySortOrderAsc(projectId).size();
        if (currentCount >= MAX_IMAGES_PER_PROJECT) {
            throw new IllegalStateException("Mỗi dự án tối đa " + MAX_IMAGES_PER_PROJECT + " ảnh");
        }

        String url = minioFileUploadService.store(file);

        PortfolioMedia media = new PortfolioMedia();
        media.setProjectId(projectId);
        media.setUrl(url);
        media.setMediaType("IMAGE");
        media.setSortOrder((int) currentCount);

        media = mediaRepository.save(media);

        return toMediaDto(media);
    }

    @Transactional
    public void deleteMedia(UUID userId, UUID projectId, UUID mediaId) {
        PortfolioProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dự án portfolio"));

        if (!project.getUserId().equals(userId)) {
            throw new SecurityException("Không có quyền truy cập dự án này");
        }

        PortfolioMedia media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ảnh"));

        if (!media.getProjectId().equals(projectId)) {
            throw new SecurityException("Ảnh không thuộc dự án này");
        }

        minioFileUploadService.delete(media.getUrl());
        mediaRepository.delete(media);
    }

    @Transactional
    public void reorderMedia(UUID userId, UUID projectId, List<UUID> mediaIds) {
        PortfolioProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dự án portfolio"));

        if (!project.getUserId().equals(userId)) {
            throw new SecurityException("Không có quyền truy cập dự án này");
        }

        List<PortfolioMedia> mediaList = mediaRepository.findByProjectIdOrderBySortOrderAsc(projectId);
        List<UUID> projectMediaIds = mediaList.stream().map(PortfolioMedia::getId).collect(Collectors.toList());

        if (!projectMediaIds.containsAll(mediaIds) || mediaIds.size() != projectMediaIds.size()) {
            throw new IllegalArgumentException("Danh sách ảnh không hợp lệ");
        }

        for (int i = 0; i < mediaIds.size(); i++) {
            UUID id = mediaIds.get(i);
            PortfolioMedia media = mediaList.stream().filter(m -> m.getId().equals(id)).findFirst().orElse(null);
            if (media != null) {
                media.setSortOrder(i);
                mediaRepository.save(media);
            }
        }
    }

    private void updateProjectFromDto(PortfolioProject project, PortfolioProjectDto dto) {
        updateCommonFields(project, dto);
        // For manual projects, also allow updating internshipId if provided
        if (dto.getInternshipId() != null) {
            project.setInternshipId(dto.getInternshipId());
        }
    }

    private void updateIocProjectFields(PortfolioProject project, PortfolioProjectDto dto) {
        // Only allow updating specific fields for IOC projects
        // Cannot change: source, sourceRef, internshipId
        updateCommonFields(project, dto);
    }

    private void updateCommonFields(PortfolioProject project, PortfolioProjectDto dto) {
        project.setName(dto.getName());
        project.setRole(dto.getRole());
        project.setDescription(dto.getDescription());
        project.setTechStack(dto.getTechStack());
        project.setRepoUrl(validateAndNormalizeUrl(dto.getRepoUrl()));
        project.setDemoUrl(validateAndNormalizeUrl(dto.getDemoUrl()));
        project.setStartDate(dto.getStartDate());
        project.setEndDate(dto.getEndDate());
        project.setVisible(dto.isVisible());
        project.setSortOrder(dto.getSortOrder());
    }

    private String validateAndNormalizeUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        String trimmed = url.trim();
        try {
            new URL(trimmed); // Validates URL format
            return trimmed;
        } catch (Exception e) {
            throw new IllegalArgumentException("URL không hợp lệ: " + trimmed);
        }
    }

    private PortfolioProjectDto toDto(PortfolioProject project) {
        PortfolioProjectDto dto = new PortfolioProjectDto();
        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setRole(project.getRole());
        dto.setDescription(project.getDescription());
        dto.setTechStack(project.getTechStack());
        dto.setRepoUrl(project.getRepoUrl());
        dto.setDemoUrl(project.getDemoUrl());
        dto.setStartDate(project.getStartDate());
        dto.setEndDate(project.getEndDate());
        dto.setVisible(project.isVisible());
        dto.setSortOrder(project.getSortOrder());
        dto.setInternshipId(project.getInternshipId());
        dto.setSource(project.getSource());
        dto.setSourceRef(project.getSourceRef());

        List<PortfolioMedia> mediaList = mediaRepository.findByProjectIdOrderBySortOrderAsc(project.getId());
        dto.setMedia(mediaList.stream().map(this::toMediaDto).collect(Collectors.toList()));

        return dto;
    }

    private PortfolioProjectDto.PortfolioMediaDto toMediaDto(PortfolioMedia media) {
        PortfolioProjectDto.PortfolioMediaDto dto = new PortfolioProjectDto.PortfolioMediaDto();
        dto.setId(media.getId());
        dto.setUrl(media.getUrl());
        dto.setMediaType(media.getMediaType());
        dto.setSortOrder(media.getSortOrder());
        return dto;
    }
}