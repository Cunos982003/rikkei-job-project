package com.example.learningservice.service;

import com.example.learningservice.model.dto.response.staff.CandidateInfoDto;
import com.example.learningservice.model.dto.response.staff.EnrollmentProgressDto;
import com.example.learningservice.model.dto.response.staff.MissingSkillDto;
import com.example.learningservice.model.dto.response.staff.UpskillingCandidateDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelExportServiceTest {

    private final ExcelExportService excelExportService = new ExcelExportService();

    @Test
    @DisplayName("Sinh tệp .xlsx hợp lệ chứa đúng dữ liệu ứng viên và thoát ký tự XML an toàn")
    void exportUpskillingCandidates_generatesValidXlsxZipWithData() throws Exception {
        UpskillingCandidateDto candidate = UpskillingCandidateDto.builder()
                .candidate(CandidateInfoDto.builder()
                        .userId(UUID.randomUUID())
                        .fullName("Nguyễn Văn An & Cộng sự")
                        .email("an.nv@rikkeijobs.test")
                        .phone("0912345600")
                        .build())
                .classCode("RA-JAVA-2026")
                .courseName("Khóa đào tạo Kỹ sư Phần mềm Java")
                .target("Java Backend Fresher")
                .missingSkills(List.of(
                        MissingSkillDto.builder()
                                .skillId("JAVA_CORE")
                                .skillName("Java Core")
                                .required(80)
                                .current(55)
                                .gap(25)
                                .priority("HIGH")
                                .build()
                ))
                .enrollments(List.of(
                        EnrollmentProgressDto.builder()
                                .course("Lập trình Spring Boot nâng cao & Microservices")
                                .progress(new BigDecimal("100.00"))
                                .score(new BigDecimal("85.00"))
                                .status("COMPLETED")
                                .build()
                ))
                .readiness(75.5)
                .status("IN_PROGRESS")
                .build();

        byte[] xlsxBytes = excelExportService.exportUpskillingCandidates(List.of(candidate));

        assertThat(xlsxBytes).isNotNull();
        assertThat(xlsxBytes.length).isGreaterThan(500);
        // Magic bytes của file ZIP / XLSX: PK\u0003\u0004
        assertThat(xlsxBytes[0]).isEqualTo((byte) 'P');
        assertThat(xlsxBytes[1]).isEqualTo((byte) 'K');

        Map<String, String> entries = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(xlsxBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String content = new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                entries.put(entry.getName(), content);
            }
        }

        assertThat(entries).containsKeys(
                "[Content_Types].xml",
                "_rels/.rels",
                "xl/workbook.xml",
                "xl/_rels/workbook.xml.rels",
                "xl/styles.xml",
                "xl/worksheets/sheet1.xml"
        );

        String sheet1 = entries.get("xl/worksheets/sheet1.xml");
        assertThat(sheet1).contains("Nguyễn Văn An &amp; Cộng sự");
        assertThat(sheet1).contains("RA-JAVA-2026");
        assertThat(sheet1).contains("Java Core (thiếu 25đ)");
        assertThat(sheet1).contains("75.5");
        assertThat(sheet1).contains("Đang học");
    }

    @Test
    @DisplayName("Xuất Excel khi danh sách rỗng vẫn sinh tệp .xlsx hợp lệ có dòng tiêu đề")
    void exportUpskillingCandidates_emptyList_generatesValidXlsxWithHeaders() throws Exception {
        byte[] xlsxBytes = excelExportService.exportUpskillingCandidates(List.of());

        assertThat(xlsxBytes).isNotNull();
        assertThat(xlsxBytes[0]).isEqualTo((byte) 'P');
        assertThat(xlsxBytes[1]).isEqualTo((byte) 'K');

        String sheet1 = null;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(xlsxBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("xl/worksheets/sheet1.xml".equals(entry.getName())) {
                    sheet1 = new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }

        assertThat(sheet1).isNotNull();
        assertThat(sheet1).contains("Họ và tên");
        assertThat(sheet1).contains("Mã lớp");
    }
}
