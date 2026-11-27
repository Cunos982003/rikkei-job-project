package com.example.profileservice.service;

import com.example.profileservice.dto.CVDto;
import com.example.profileservice.entity.CV;
import com.example.profileservice.entity.CVSection;
import com.example.profileservice.entity.SectionType;
import com.example.profileservice.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import com.example.profileservice.repository.CVRepository;
import com.example.profileservice.repository.CVSectionRepository;
import com.example.profileservice.service.MinioFileUploadService;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.UnitValue;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CVService {

    private final CVRepository cvRepository;
    private final CVSectionRepository sectionRepository;
    private final ProfileDataService profileDataService;
    private final MinioFileUploadService minioFileUploadService;

    @Value("${app.public-base-url:http://localhost:8080}")
    private String publicBaseUrl;

    private static final int MAX_CVS_PER_USER = 5;
    private static final SectionType[] VALID_SECTION_TYPES = SectionType.values();

    @Transactional(readOnly = true)
    public List<CVDto> getMyCVs(UUID userId) {
        return cvRepository.findByUserIdWithSectionsOrderByCreatedAtDesc(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CVDto getMyCV(UUID userId, UUID cvId) {
        CV cv = cvRepository.findByIdWithSections(cvId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy CV"));
        if (!cv.getUserId().equals(userId)) {
            throw new AccessDeniedException("Không có quyền truy cập CV này");
        }
        return toDto(cv);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<CVDto> getCVByPublicToken(String publicToken) {
        return cvRepository.findByPublicTokenWithSections(publicToken)
                .filter(CV::isPublic)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public boolean canAccessCVPdf(UUID userId, UUID cvId) {
        return cvRepository.findById(cvId)
                .map(cv -> {
                    if (userId != null && cv.getUserId().equals(userId)) {
                        return true; // User owns the CV
                    }
                    return cv.isPublic(); // CV is public
                })
                .orElse(false);
    }

    @Transactional
    public CVDto createCV(UUID userId, CVDto.CreateCVRequest request) {
        long count = cvRepository.countByUserId(userId);
        if (count >= MAX_CVS_PER_USER) {
            throw new IllegalStateException("Tối đa " + MAX_CVS_PER_USER + " CV mỗi người dùng");
        }

        CV cv = new CV();
        cv.setUserId(userId);
        cv.setTitle(request.getTitle());
        cv.setTargetPosition(request.getTargetPosition());
        cv.setTemplate(request.getTemplate() != null ? request.getTemplate() : "RIKKEI_STANDARD");
        if (request.getIsPublic() != null) cv.setPublic(request.getIsPublic());
        cv.setPublicToken(generateUniquePublicToken());
        cv = cvRepository.save(cv);
        return toDto(cv);
    }

    @Transactional
    public CVDto updateCV(UUID userId, UUID cvId, CVDto.UpdateCVRequest request) {
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy CV"));
        if (!cv.getUserId().equals(userId)) {
            throw new AccessDeniedException("Không có quyền truy cập CV này");
        }
        if (request.getTitle() != null) cv.setTitle(request.getTitle());
        if (request.getTargetPosition() != null) cv.setTargetPosition(request.getTargetPosition());
        if (request.getTemplate() != null) cv.setTemplate(request.getTemplate());
        if (request.getIsPublic() != null) cv.setPublic(request.getIsPublic());
        cv = cvRepository.save(cv);
        return toDto(cv);
    }

    @Transactional
    public void deleteCV(UUID userId, UUID cvId) {
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy CV"));
        if (!cv.getUserId().equals(userId)) {
            throw new AccessDeniedException("Không có quyền truy cập CV này");
        }
        cvRepository.delete(cv);
    }

    @Transactional
    public CVDto.CVSectionDto updateCVSection(UUID userId, UUID cvId, CVDto.UpdateCVSectionRequest request) {
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy CV"));
        if (!cv.getUserId().equals(userId)) {
            throw new AccessDeniedException("Không có quyền truy cập CV này");
        }
        if (!Arrays.asList(VALID_SECTION_TYPES).contains(request.getSectionType())) {
            throw new IllegalArgumentException("Loại section không hợp lệ");
        }

        CVSection section = sectionRepository.findByCvIdAndSectionType(cvId, request.getSectionType())
                .orElseGet(() -> {
                    CVSection s = new CVSection();
                    s.setCv(cv);
                    s.setSectionType(request.getSectionType());
                    return s;
                });

        if (request.getItemIds() != null) section.setItemIds(request.getItemIds());
        if (request.getIncluded() != null) section.setIncluded(request.getIncluded());
        if (request.getSortOrder() != null) section.setSortOrder(request.getSortOrder());
        section = sectionRepository.save(section);
        return toSectionDto(section);
    }

    @Transactional
    public void deleteCVSection(UUID userId, UUID cvId, SectionType sectionType) {
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy CV"));
        if (!cv.getUserId().equals(userId)) {
            throw new AccessDeniedException("Không có quyền truy cập CV này");
        }
        sectionRepository.deleteByCvIdAndSectionType(cvId, sectionType);
    }

    @Transactional
    public void reorderCVSections(UUID userId, UUID cvId, CVDto.ReorderCVSectionsRequest request) {
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy CV"));
        if (!cv.getUserId().equals(userId)) {
            throw new AccessDeniedException("Không có quyền truy cập CV này");
        }
        List<CVSection> sections = sectionRepository.findByCvIdOrderBySortOrderAsc(cvId);
        List<UUID> sectionIds = sections.stream().map(CVSection::getId).collect(Collectors.toList());
        if (!sectionIds.containsAll(request.getSectionIds()) || request.getSectionIds().size() != sectionIds.size()) {
            throw new IllegalArgumentException("Danh sách section không hợp lệ");
        }
        for (int i = 0; i < request.getSectionIds().size(); i++) {
            UUID id = request.getSectionIds().get(i);
            CVSection section = sections.stream().filter(s -> s.getId().equals(id)).findFirst().orElse(null);
            if (section != null) {
                section.setSortOrder(i);
                sectionRepository.save(section);
            }
        }
    }

    private String generateUniquePublicToken() {
        String token;
        do {
            token = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        } while (cvRepository.existsByPublicToken(token));
        return token;
    }

    private CVDto toDto(CV cv) {
        CVDto dto = new CVDto();
        dto.setId(cv.getId());
        dto.setTitle(cv.getTitle());
        dto.setTargetPosition(cv.getTargetPosition());
        dto.setTemplate(cv.getTemplate());
        dto.setPublicToken(cv.getPublicToken());
        dto.setPublic(cv.isPublic());
        dto.setPdfUrl(cv.getPdfUrl());
        dto.setCreatedAt(cv.getCreatedAt());
        dto.setUpdatedAt(cv.getUpdatedAt());
        // Sections are already fetched via LEFT JOIN FETCH in repository queries
        dto.setSections(cv.getSections().stream().map(this::toSectionDto).collect(Collectors.toList()));
        return dto;
    }

    private CVDto.CVSectionDto toSectionDto(CVSection section) {
        CVDto.CVSectionDto dto = new CVDto.CVSectionDto();
        dto.setId(section.getId());
        dto.setSectionType(section.getSectionType());
        dto.setItemIds(section.getItemIds());
        dto.setIncluded(section.isIncluded());
        dto.setSortOrder(section.getSortOrder());
        return dto;
    }

    @Transactional(readOnly = true)
    public CVDto.CVPreviewResponse getCVPreview(UUID userId, UUID cvId) {
        CV cv = cvRepository.findByIdWithSections(cvId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy CV"));
        if (!cv.getUserId().equals(userId)) {
            throw new AccessDeniedException("Không có quyền truy cập CV này");
        }

        // Sections are already fetched via LEFT JOIN FETCH
        List<CVDto.SectionPreviewData> previewSections = cv.getSections().stream()
                .filter(CVSection::isIncluded)
                .map(this::buildSectionPreview)
                .collect(Collectors.toList());

        CVDto.CVPreviewResponse response = new CVDto.CVPreviewResponse();
        response.setId(cv.getId());
        response.setTitle(cv.getTitle());
        response.setTargetPosition(cv.getTargetPosition());
        response.setTemplate(cv.getTemplate());
        response.setPublicToken(cv.getPublicToken());
        response.setPublic(cv.isPublic());
        response.setSections(previewSections);
        return response;
    }

    private CVDto.SectionPreviewData buildSectionPreview(CVSection section) {
        UUID userId = section.getCv().getUserId();
        Object data = switch (section.getSectionType()) {
            case SUMMARY -> profileDataService.getSummary(userId);
            case SKILLS -> profileDataService.getSkills(userId, section.getItemIds());
            case EDUCATION -> profileDataService.getEducation(userId, section.getItemIds());
            case PROJECTS -> profileDataService.getProjects(userId, section.getItemIds());
            case CERTIFICATES -> profileDataService.getCertificates(userId, section.getItemIds());
            case INTERNSHIP -> profileDataService.getInternships(userId, section.getItemIds());
        };

        CVDto.SectionPreviewData preview = new CVDto.SectionPreviewData();
        preview.setSectionType(section.getSectionType());
        preview.setIncluded(section.isIncluded());
        preview.setSortOrder(section.getSortOrder());
        preview.setData(data);
        return preview;
    }

    @Transactional
    public CVDto.GeneratePDFResponse generatePDF(UUID userId, UUID cvId) throws IOException {
        CV cv = cvRepository.findByIdWithSections(cvId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy CV"));
        if (!cv.getUserId().equals(userId)) {
            throw new AccessDeniedException("Không có quyền truy cập CV này");
        }

        // Generate PDF in memory
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc, PageSize.A4);
        document.setMargins(36, 36, 72, 36); // top, right, bottom, left

        // Register Vietnamese-compatible font (Noto Sans) from classpath
        PdfFont vietnameseFont;
        try (InputStream fontStream = new ClassPathResource("fonts/NotoSans-Vietnamese.ttf").getInputStream()) {
            byte[] fontBytes = fontStream.readAllBytes();
            vietnameseFont = PdfFontFactory.createFont(fontBytes, PdfEncodings.IDENTITY_H, true);
        } catch (IOException e) {
            log.warn("Could not load NotoSans-Vietnamese.ttf from classpath, falling back to Helvetica", e);
            vietnameseFont = PdfFontFactory.createFont(com.itextpdf.io.font.FontConstants.HELVETICA, PdfEncodings.IDENTITY_H, true);
        }
        document.setFont(vietnameseFont);

        // Add CV header
        addCVHeader(document, cv);

        // Add content from sections (reading live data)
        // Sections are already fetched via LEFT JOIN FETCH
        for (CVSection section : cv.getSections()) {
            if (!section.isIncluded()) continue;
            addSectionToDocument(document, section);
        }

        // Generate QR code for public URL
        String publicUrl = publicBaseUrl + "/p/" + cv.getPublicToken();
        addQRCodeToDocument(document, pdfDoc, publicUrl);

        document.close();

        // Upload to MinIO
        String objectName = "cvs/" + cv.getId() + ".pdf";
        minioFileUploadService.store(objectName, new ByteArrayInputStream(baos.toByteArray()), "application/pdf");

        // Update CV with PDF URL
        cv.setPdfUrl(objectName);
        cvRepository.save(cv);

        CVDto.GeneratePDFResponse response = new CVDto.GeneratePDFResponse();
        response.setPdfUrl(objectName);
        response.setPublicUrl(publicUrl);
        return response;
    }

    private void addCVHeader(Document document, CV cv) {
        // Title
        Paragraph title = new Paragraph(cv.getTitle())
                .setFontSize(24)
                .setBold()
                .setFontColor(ColorConstants.BLUE)
                .setMarginBottom(4);
        document.add(title);

        // Target position
        if (cv.getTargetPosition() != null && !cv.getTargetPosition().isBlank()) {
            Paragraph targetPos = new Paragraph("Vị trí ứng tuyển: " + cv.getTargetPosition())
                    .setFontSize(12)
                    .setFontColor(ColorConstants.DARK_GRAY)
                    .setMarginBottom(8);
            document.add(targetPos);
        }

        // Template info
        Paragraph templateInfo = new Paragraph("Mẫu: " + (cv.getTemplate() != null ? cv.getTemplate() : "RIKKEI_STANDARD"))
                .setFontSize(10)
                .setFontColor(ColorConstants.GRAY)
                .setMarginBottom(16);
        document.add(templateInfo);

        // Line separator
        Table lineTable = new Table(UnitValue.createPercentArray(new float[]{100}))
                .useAllAvailableWidth()
                .setBorder(new SolidBorder(ColorConstants.BLUE, 1))
                .setMarginBottom(16)
                .setMarginTop(0);
        lineTable.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(0));
        document.add(lineTable);
    }

    private void addSectionToDocument(Document document, CVSection section) {
        try {
            UUID userId = section.getCv().getUserId();

            // Add section title
            String sectionTitle = getSectionTitle(section.getSectionType());
            if (sectionTitle != null) {
                Paragraph title = new Paragraph(sectionTitle)
                        .setFontSize(14)
                        .setBold()
                        .setFontColor(ColorConstants.BLUE)
                        .setMarginBottom(8)
                        .setMarginTop(16);
                document.add(title);

                // Add a line under title using a table with border
                Table lineTable = new Table(UnitValue.createPercentArray(new float[]{100}))
                        .useAllAvailableWidth()
                        .setBorder(new SolidBorder(ColorConstants.BLUE, 1))
                        .setMarginBottom(8)
                        .setMarginTop(0);
                lineTable.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(0));
                document.add(lineTable);
                document.add(new Paragraph("\n"));
            }

            // Render content based on section type
            switch (section.getSectionType()) {
                case SUMMARY -> renderSummary(document, userId);
                case SKILLS -> renderSkills(document, userId, section.getItemIds());
                case EDUCATION -> renderEducation(document, userId, section.getItemIds());
                case PROJECTS -> renderProjects(document, userId, section.getItemIds());
                case CERTIFICATES -> renderCertificates(document, userId, section.getItemIds());
                case INTERNSHIP -> renderInternships(document, userId, section.getItemIds());
            }
        } catch (Exception e) {
            // Log error but continue with other sections
            log.error("Error rendering section {}: {}", section.getSectionType(), e.getMessage(), e);
        }
    }

    private String getSectionTitle(SectionType sectionType) {
        return switch (sectionType) {
            case SUMMARY -> "TÓM TẮT";
            case SKILLS -> "KỸ NĂNG";
            case EDUCATION -> "HỌC VẤN";
            case PROJECTS -> "DỰ ÁN";
            case CERTIFICATES -> "CHỨNG CHỈ";
            case INTERNSHIP -> "THỰC TẬP";
        };
    }

    private void renderSummary(Document document, UUID userId) {
        CVDto.SummaryPreview summary = profileDataService.getSummary(userId);
        if (summary.getSummary() != null && !summary.getSummary().isBlank()) {
            Paragraph summaryPara = new Paragraph(summary.getSummary())
                    .setFontSize(10)
                    .setMarginBottom(12);
            document.add(summaryPara);
        }
    }

    private void renderSkills(Document document, UUID userId, List<UUID> itemIds) {
        List<CVDto.SkillPreview> skills = profileDataService.getSkills(userId, itemIds);
        for (CVDto.SkillPreview skill : skills) {
            Paragraph skillPara = new Paragraph()
                    .add(new Text(skill.getSkillName() + "  ").setFontSize(10).setBold())
                    .add(new Text("[" + skill.getVerificationStatus() + "]  ").setFontSize(9).setFontColor(ColorConstants.BLUE))
                    .add(new Text(skill.getLevel()).setFontSize(10).setFontColor(ColorConstants.GRAY))
                    .setMarginBottom(4);
            document.add(skillPara);
        }
    }

    private void renderEducation(Document document, UUID userId, List<UUID> itemIds) {
        List<CVDto.EducationPreview> educations = profileDataService.getEducation(userId, itemIds);
        for (CVDto.EducationPreview edu : educations) {
            Paragraph school = new Paragraph(edu.getSchool())
                    .setFontSize(11)
                    .setBold()
                    .setMarginBottom(2);
            document.add(school);

            String major = edu.getMajor() != null ? edu.getMajor() : "";
            String dateRange = formatDateRange(edu.getFromDate(), edu.getToDate());
            Paragraph details = new Paragraph()
                    .add(new Text(major + "  ").setFontSize(10))
                    .add(new Text(dateRange).setFontSize(10).setFontColor(ColorConstants.GRAY))
                    .setMarginBottom(4);
            document.add(details);

            if (edu.getGpa() != null) {
                Paragraph gpa = new Paragraph("GPA: " + edu.getGpa())
                        .setFontSize(10)
                        .setFontColor(ColorConstants.GRAY)
                        .setMarginBottom(8);
                document.add(gpa);
            }
            if (edu.getNote() != null) {
                Paragraph note = new Paragraph(edu.getNote())
                        .setFontSize(10)
                        .setFontColor(ColorConstants.GRAY)
                        .setMarginBottom(8);
                document.add(note);
            }
        }
    }

    private void renderProjects(Document document, UUID userId, List<UUID> itemIds) {
        List<CVDto.ProjectPreview> projects = profileDataService.getProjects(userId, itemIds);
        for (CVDto.ProjectPreview proj : projects) {
            Paragraph name = new Paragraph(proj.getName())
                    .setFontSize(11)
                    .setBold()
                    .setMarginBottom(2);
            document.add(name);

            if (proj.getRole() != null) {
                Paragraph role = new Paragraph("Vai trò: " + proj.getRole())
                        .setFontSize(10)
                        .setFontColor(ColorConstants.BLUE)
                        .setMarginBottom(2);
                document.add(role);
            }

            if (proj.getTechStack() != null) {
                Paragraph tech = new Paragraph("Công nghệ: " + proj.getTechStack())
                        .setFontSize(10)
                        .setFontColor(ColorConstants.GRAY)
                        .setMarginBottom(2);
                document.add(tech);
            }

            if (proj.getDescription() != null) {
                Paragraph desc = new Paragraph(proj.getDescription())
                        .setFontSize(10)
                        .setMarginBottom(4);
                document.add(desc);
            }

            String dateRange = formatDateRange(proj.getStartDate(), proj.getEndDate());
            if (!dateRange.isEmpty()) {
                Paragraph dates = new Paragraph(dateRange)
                        .setFontSize(10)
                        .setFontColor(ColorConstants.GRAY)
                        .setMarginBottom(2);
                document.add(dates);
            }

            if (proj.getRepoUrl() != null) {
                Paragraph repo = new Paragraph("Repo: " + proj.getRepoUrl())
                        .setFontSize(10)
                        .setFontColor(ColorConstants.BLUE)
                        .setMarginBottom(2);
                document.add(repo);
            }
            if (proj.getDemoUrl() != null) {
                Paragraph demo = new Paragraph("Demo: " + proj.getDemoUrl())
                        .setFontSize(10)
                        .setFontColor(ColorConstants.BLUE)
                        .setMarginBottom(8);
                document.add(demo);
            }
        }
    }

    private void renderCertificates(Document document, UUID userId, List<UUID> itemIds) {
        List<CVDto.CertificatePreview> certificates = profileDataService.getCertificates(userId, itemIds);
        for (CVDto.CertificatePreview cert : certificates) {
            Paragraph name = new Paragraph(cert.getName())
                    .setFontSize(11)
                    .setBold()
                    .setMarginBottom(2);
            document.add(name);

            Paragraph issuer = new Paragraph(cert.getIssuer())
                    .setFontSize(10)
                    .setFontColor(ColorConstants.BLUE)
                    .setMarginBottom(2);
            document.add(issuer);

            String dateRange = formatDateRange(cert.getIssuedAt(), cert.getExpiresAt());
            Paragraph dates = new Paragraph(dateRange)
                    .setFontSize(10)
                    .setFontColor(ColorConstants.GRAY)
                    .setMarginBottom(8);
            document.add(dates);
        }
    }

    private void renderInternships(Document document, UUID userId, List<UUID> itemIds) {
        List<CVDto.InternshipPreview> internships = profileDataService.getInternships(userId, itemIds);
        for (CVDto.InternshipPreview internship : internships) {
            Paragraph topic = new Paragraph(internship.getTopicName())
                    .setFontSize(11)
                    .setBold()
                    .setMarginBottom(2);
            document.add(topic);

            if (internship.getGroupName() != null) {
                Paragraph group = new Paragraph("Nhóm: " + internship.getGroupName())
                        .setFontSize(10)
                        .setFontColor(ColorConstants.BLUE)
                        .setMarginBottom(2);
                document.add(group);
            }

            String dateRange = formatDateRange(internship.getStartDate(), internship.getEndDate());
            Paragraph dates = new Paragraph(dateRange)
                    .setFontSize(10)
                    .setFontColor(ColorConstants.GRAY)
                    .setMarginBottom(4);
            document.add(dates);

            if (internship.getFinalScore() != null) {
                Paragraph score = new Paragraph("Điểm: " + internship.getFinalScore())
                        .setFontSize(10)
                        .setFontColor(ColorConstants.GRAY)
                        .setMarginBottom(4);
                document.add(score);
            }

            if (internship.getTasks() != null && !internship.getTasks().isEmpty()) {
                for (CVDto.InternshipTaskPreview task : internship.getTasks()) {
                    Paragraph taskPara = new Paragraph("• " + task.getTitle())
                            .setFontSize(10)
                            .setMarginBottom(2);
                    if (task.getAcceptedScore() != null) {
                        taskPara.add(new Text(" - Điểm: " + task.getAcceptedScore()).setFontColor(ColorConstants.GRAY));
                    }
                    document.add(taskPara);
                }
            }
            document.add(new Paragraph("\n"));
        }
    }

    private String formatDateRange(String from, String to) {
        if (from == null || from.isBlank()) return "";
        if (to == null || to.isBlank()) return from + " - Hiện tại";
        return from + " - " + to;
    }

    private void addQRCodeToDocument(Document document, PdfDocument pdfDoc, String publicUrl) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(publicUrl, BarcodeFormat.QR_CODE, 200, 200);
            ByteArrayOutputStream qrOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", qrOutputStream);

            com.itextpdf.io.image.ImageData imageData = ImageDataFactory.create(qrOutputStream.toByteArray());
            Image qrImage = new Image(imageData)
                    .setWidth(80)
                    .setHeight(80);

            // Add QR code at bottom right of last page using fixed position
            // This avoids creating an extra blank page because it's positioned absolutely
            int pageCount = pdfDoc.getNumberOfPages();
            qrImage.setFixedPosition(pageCount,
                    PageSize.A4.getWidth() - 116, // right margin 36
                    36, // bottom margin 36
                    80);

            // Add label under QR code
            Paragraph qrLabel = new Paragraph("Quét để xem CV online")
                    .setFontSize(8)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.GRAY)
                    .setFixedPosition(pageCount,
                            PageSize.A4.getWidth() - 156,
                            20,
                            120);

            document.add(qrImage);
            document.add(qrLabel);
        } catch (Exception e) {
            log.error("Error generating QR code: {}", e.getMessage(), e);
        }
    }
}