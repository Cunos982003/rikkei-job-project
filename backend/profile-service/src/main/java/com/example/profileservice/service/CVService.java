package com.example.profileservice.service;

import com.example.profileservice.dto.CVDto;
import com.example.profileservice.entity.CV;
import com.example.profileservice.entity.CVSection;
import com.example.profileservice.exception.ResourceNotFoundException;
import com.example.profileservice.repository.CVRepository;
import com.example.profileservice.repository.CVSectionRepository;
import com.example.profileservice.service.MinioFileUploadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.io.font.FontProgramFactory;
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
import com.itextpdf.layout.property.VerticalAlignment;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CVService {

    private final CVRepository cvRepository;
    private final CVSectionRepository sectionRepository;
    private final MinioFileUploadService minioFileUploadService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.public-base-url:http://localhost:8080}")
    private String publicBaseUrl;

    private static final int MAX_CVS_PER_USER = 5;
    private static final String[] VALID_SECTION_TYPES = {
            "personal_info", "education", "experience", "skills",
            "projects", "certificates", "summary"
    };

    @Transactional(readOnly = true)
    public List<CVDto> getMyCVs(UUID userId) {
        return cvRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CVDto getMyCV(UUID userId, UUID cvId) {
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy CV"));
        if (!cv.getUserId().equals(userId)) {
            throw new SecurityException("Không có quyền truy cập CV này");
        }
        return toDto(cv);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<CVDto> getCVByPublicToken(String publicToken) {
        return cvRepository.findByPublicToken(publicToken)
                .filter(CV::isPublic)
                .map(this::toDto);
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
        cv.setPublicToken(generateUniquePublicToken());
        cv = cvRepository.save(cv);
        return toDto(cv);
    }

    @Transactional
    public CVDto updateCV(UUID userId, UUID cvId, CVDto.UpdateCVRequest request) {
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy CV"));
        if (!cv.getUserId().equals(userId)) {
            throw new SecurityException("Không có quyền truy cập CV này");
        }
        if (request.getTitle() != null) cv.setTitle(request.getTitle());
        if (request.getIsPublic() != null) cv.setPublic(request.getIsPublic());
        cv = cvRepository.save(cv);
        return toDto(cv);
    }

    @Transactional
    public void deleteCV(UUID userId, UUID cvId) {
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy CV"));
        if (!cv.getUserId().equals(userId)) {
            throw new SecurityException("Không có quyền truy cập CV này");
        }
        cvRepository.delete(cv);
    }

    @Transactional
    public CVDto.CVSectionDto updateCVSection(UUID userId, UUID cvId, CVDto.UpdateCVSectionRequest request) {
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy CV"));
        if (!cv.getUserId().equals(userId)) {
            throw new SecurityException("Không có quyền truy cập CV này");
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
        section.setContentJson(request.getContentJson());
        if (request.getSortOrder() != null) section.setSortOrder(request.getSortOrder());
        section = sectionRepository.save(section);
        return toSectionDto(section);
    }

    @Transactional
    public void deleteCVSection(UUID userId, UUID cvId, String sectionType) {
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy CV"));
        if (!cv.getUserId().equals(userId)) {
            throw new SecurityException("Không có quyền truy cập CV này");
        }
        sectionRepository.deleteByCvIdAndSectionType(cvId, sectionType);
    }

    @Transactional
    public void reorderCVSections(UUID userId, UUID cvId, CVDto.ReorderCVSectionsRequest request) {
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy CV"));
        if (!cv.getUserId().equals(userId)) {
            throw new SecurityException("Không có quyền truy cập CV này");
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
        dto.setPublicToken(cv.getPublicToken());
        dto.setPublic(cv.isPublic());
        dto.setPdfUrl(cv.getPdfUrl());
        dto.setCreatedAt(cv.getCreatedAt());
        dto.setUpdatedAt(cv.getUpdatedAt());
        List<CVSection> sections = sectionRepository.findByCvIdOrderBySortOrderAsc(cv.getId());
        dto.setSections(sections.stream().map(this::toSectionDto).collect(Collectors.toList()));
        return dto;
    }

    private CVDto.CVSectionDto toSectionDto(CVSection section) {
        CVDto.CVSectionDto dto = new CVDto.CVSectionDto();
        dto.setId(section.getId());
        dto.setSectionType(section.getSectionType());
        dto.setContentJson(section.getContentJson());
        dto.setSortOrder(section.getSortOrder());
        return dto;
    }

    @Transactional
    public CVDto.GeneratePDFResponse generatePDF(UUID userId, UUID cvId) throws IOException {
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy CV"));
        if (!cv.getUserId().equals(userId)) {
            throw new SecurityException("Không có quyền truy cập CV này");
        }

        // Generate PDF in memory
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc, PageSize.A4);
        document.setMargins(36, 36, 72, 36); // top, right, bottom, left

        // Register Vietnamese-compatible font (DejaVu Sans)
        PdfFont vietnameseFont = PdfFontFactory.createFont(
                "fonts/NotoSans-Vietnamese.ttf",
                PdfEncodings.IDENTITY_H,
                true
        );
        document.setFont(vietnameseFont);

        // Add content from sections
        List<CVSection> sections = sectionRepository.findByCvIdOrderBySortOrderAsc(cvId);
        for (CVSection section : sections) {
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

    private void addSectionToDocument(Document document, CVSection section) {
        try {
            Map<String, Object> content = objectMapper.readValue(section.getContentJson(), Map.class);

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
                case "personal_info":
                    renderPersonalInfo(document, content);
                    break;
                case "education":
                    renderEducation(document, content);
                    break;
                case "experience":
                    renderExperience(document, content);
                    break;
                case "skills":
                    renderSkills(document, content);
                    break;
                case "projects":
                    renderProjects(document, content);
                    break;
                case "certificates":
                    renderCertificates(document, content);
                    break;
                case "summary":
                    renderSummary(document, content);
                    break;
            }
        } catch (Exception e) {
            // Log error but continue with other sections
            System.err.println("Error rendering section " + section.getSectionType() + ": " + e.getMessage());
        }
    }

    private String getSectionTitle(String sectionType) {
        return switch (sectionType) {
            case "personal_info" -> "THÔNG TIN CÁ NHÂN";
            case "education" -> "HỌC VẤN";
            case "experience" -> "KINH NGHIỆM LÀM VIỆC";
            case "skills" -> "KỸ NĂNG";
            case "projects" -> "DỰ ÁN";
            case "certificates" -> "CHỨNG CHỈ";
            case "summary" -> "TÓM TẮT";
            default -> null;
        };
    }

    private void renderPersonalInfo(Document document, Map<String, Object> content) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                .useAllAvailableWidth()
                .setMarginBottom(12);

        addTableRow(table, "Họ tên", getString(content, "fullName"));
        addTableRow(table, "Email", getString(content, "email"));
        addTableRow(table, "Số điện thoại", getString(content, "phone"));
        addTableRow(table, "Địa chỉ", getString(content, "address"));
        addTableRow(table, "Ngày sinh", getString(content, "dob"));
        addTableRow(table, "Giới tính", getString(content, "gender"));
        addTableRow(table, "Headline", getString(content, "headline"));

        document.add(table);
    }

    private void renderEducation(Document document, Map<String, Object> content) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> educations = (List<Map<String, Object>>) content.get("educations");
        if (educations == null) return;

        for (Map<String, Object> edu : educations) {
            Paragraph school = new Paragraph(getString(edu, "school"))
                    .setFontSize(11)
                    .setBold()
                    .setMarginBottom(2);
            document.add(school);

            Paragraph details = new Paragraph()
                    .add(new Text(getString(edu, "major") + "  ").setFontSize(10))
                    .add(new Text(formatDateRange(edu, "fromDate", "toDate")).setFontSize(10).setFontColor(ColorConstants.GRAY))
                    .setMarginBottom(4);
            document.add(details);

            if (edu.containsKey("gpa") && edu.get("gpa") != null) {
                Paragraph gpa = new Paragraph("GPA: " + edu.get("gpa"))
                        .setFontSize(10)
                        .setFontColor(ColorConstants.GRAY)
                        .setMarginBottom(8);
                document.add(gpa);
            }
            if (getString(edu, "note") != null) {
                Paragraph note = new Paragraph(getString(edu, "note"))
                        .setFontSize(10)
                        .setFontColor(ColorConstants.GRAY)
                        .setMarginBottom(8);
                document.add(note);
            }
        }
    }

    private void renderExperience(Document document, Map<String, Object> content) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> experiences = (List<Map<String, Object>>) content.get("experiences");
        if (experiences == null) return;

        for (Map<String, Object> exp : experiences) {
            Paragraph position = new Paragraph(getString(exp, "position"))
                    .setFontSize(11)
                    .setBold()
                    .setMarginBottom(2);
            document.add(position);

            Paragraph company = new Paragraph(getString(exp, "company"))
                    .setFontSize(10)
                    .setFontColor(ColorConstants.BLUE)
                    .setMarginBottom(2);
            document.add(company);

            Paragraph dates = new Paragraph(formatDateRange(exp, "fromDate", "toDate"))
                    .setFontSize(10)
                    .setFontColor(ColorConstants.GRAY)
                    .setMarginBottom(4);
            document.add(dates);

            if (getString(exp, "description") != null) {
                Paragraph desc = new Paragraph(getString(exp, "description"))
                        .setFontSize(10)
                        .setMarginBottom(8);
                document.add(desc);
            }
        }
    }

    private void renderSkills(Document document, Map<String, Object> content) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> skills = (List<Map<String, Object>>) content.get("skills");
        if (skills == null) return;

        for (Map<String, Object> skill : skills) {
            Paragraph skillPara = new Paragraph()
                    .add(new Text(getString(skill, "name") + "  ").setFontSize(10).setBold())
                    .add(new Text(getString(skill, "level")).setFontSize(10).setFontColor(ColorConstants.GRAY))
                    .setMarginBottom(4);
            document.add(skillPara);
        }
    }

    private void renderProjects(Document document, Map<String, Object> content) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> projects = (List<Map<String, Object>>) content.get("projects");
        if (projects == null) return;

        for (Map<String, Object> proj : projects) {
            Paragraph name = new Paragraph(getString(proj, "name"))
                    .setFontSize(11)
                    .setBold()
                    .setMarginBottom(2);
            document.add(name);

            if (getString(proj, "role") != null) {
                Paragraph role = new Paragraph(getString(proj, "role"))
                        .setFontSize(10)
                        .setFontColor(ColorConstants.BLUE)
                        .setMarginBottom(2);
                document.add(role);
            }

            if (getString(proj, "techStack") != null) {
                Paragraph tech = new Paragraph("Công nghệ: " + getString(proj, "techStack"))
                        .setFontSize(10)
                        .setFontColor(ColorConstants.GRAY)
                        .setMarginBottom(2);
                document.add(tech);
            }

            if (getString(proj, "description") != null) {
                Paragraph desc = new Paragraph(getString(proj, "description"))
                        .setFontSize(10)
                        .setMarginBottom(4);
                document.add(desc);
            }

            if (getString(proj, "repoUrl") != null) {
                Paragraph repo = new Paragraph("Repo: " + getString(proj, "repoUrl"))
                        .setFontSize(10)
                        .setFontColor(ColorConstants.BLUE)
                        .setMarginBottom(2);
                document.add(repo);
            }
            if (getString(proj, "demoUrl") != null) {
                Paragraph demo = new Paragraph("Demo: " + getString(proj, "demoUrl"))
                        .setFontSize(10)
                        .setFontColor(ColorConstants.BLUE)
                        .setMarginBottom(8);
                document.add(demo);
            }
        }
    }

    private void renderCertificates(Document document, Map<String, Object> content) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> certificates = (List<Map<String, Object>>) content.get("certificates");
        if (certificates == null) return;

        for (Map<String, Object> cert : certificates) {
            Paragraph name = new Paragraph(getString(cert, "name"))
                    .setFontSize(11)
                    .setBold()
                    .setMarginBottom(2);
            document.add(name);

            Paragraph issuer = new Paragraph(getString(cert, "issuer"))
                    .setFontSize(10)
                    .setFontColor(ColorConstants.BLUE)
                    .setMarginBottom(2);
            document.add(issuer);

            Paragraph dates = new Paragraph()
                    .add(new Text("Ngày cấp: " + getString(cert, "issuedAt") + "  ").setFontSize(10).setFontColor(ColorConstants.GRAY))
                    .add(new Text(getString(cert, "expiresAt") != null ? "Hết hạn: " + getString(cert, "expiresAt") : "").setFontSize(10).setFontColor(ColorConstants.GRAY))
                    .setMarginBottom(8);
            document.add(dates);
        }
    }

    private void renderSummary(Document document, Map<String, Object> content) {
        String summary = getString(content, "summary");
        if (summary != null) {
            Paragraph summaryPara = new Paragraph(summary)
                    .setFontSize(10)
                    .setMarginBottom(12);
            document.add(summaryPara);
        }
    }

    private void addTableRow(Table table, String label, String value) {
        if (value == null || value.isBlank()) return;
        Cell labelCell = new Cell().add(new Paragraph(label).setFontSize(10).setBold()).setBorder(Border.NO_BORDER).setPaddingBottom(4);
        Cell valueCell = new Cell().add(new Paragraph(value).setFontSize(10)).setBorder(Border.NO_BORDER).setPaddingBottom(4);
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private String formatDateRange(Map<String, Object> map, String fromKey, String toKey) {
        String from = getString(map, fromKey);
        String to = getString(map, toKey);
        if (from == null) return "";
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

            // Add QR code at bottom right of last page
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
            System.err.println("Error generating QR code: " + e.getMessage());
        }
    }
}