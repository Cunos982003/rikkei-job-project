package com.example.learningservice.service;

import com.example.learningservice.model.dto.response.staff.EnrollmentProgressDto;
import com.example.learningservice.model.dto.response.staff.MissingSkillDto;
import com.example.learningservice.model.dto.response.staff.UpskillingCandidateDto;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Dịch vụ xuất báo cáo danh sách đào tạo bổ sung ra định dạng Excel (.xlsx) chuẩn OpenXML.
 * Sử dụng ZipOutputStream thuần Java để đảm bảo tính ổn định, nhẹ và không phụ thuộc thư viện ngoài.
 */
@Service
public class ExcelExportService {

    private static final String[] HEADERS = {
            "STT",
            "Họ và tên",
            "Email",
            "Số điện thoại",
            "Mã lớp",
            "Khoá đào tạo",
            "Mục tiêu nghề nghiệp",
            "Kỹ năng còn thiếu",
            "Khoá học bổ sung & Tiến độ",
            "Độ sẵn sàng (%)",
            "Trạng thái"
    };

    private static final double[] COL_WIDTHS = {
            6, 24, 28, 15, 16, 36, 26, 36, 44, 16, 18
    };

    public byte[] exportUpskillingCandidates(List<UpskillingCandidateDto> candidates) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {

            writeEntry(zos, "[Content_Types].xml", buildContentTypesXml());
            writeEntry(zos, "_rels/.rels", buildRelsXml());
            writeEntry(zos, "xl/workbook.xml", buildWorkbookXml());
            writeEntry(zos, "xl/_rels/workbook.xml.rels", buildWorkbookRelsXml());
            writeEntry(zos, "xl/styles.xml", buildStylesXml());
            writeEntry(zos, "xl/worksheets/sheet1.xml", buildSheet1Xml(candidates));

            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Không thể khởi tạo tệp Excel báo cáo đào tạo bổ sung", e);
        }
    }

    private void writeEntry(ZipOutputStream zos, String name, String content) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        zos.putNextEntry(entry);
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private String buildContentTypesXml() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
                </Types>
                """;
    }

    private String buildRelsXml() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                </Relationships>
                """;
    }

    private String buildWorkbookXml() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <sheets>
                    <sheet name="Dao tao bo sung" sheetId="1" r:id="rId1"/>
                  </sheets>
                </workbook>
                """;
    }

    private String buildWorkbookRelsXml() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
                </Relationships>
                """;
    }

    private String buildStylesXml() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <fonts count="2">
                    <font><sz val="11"/><name val="Calibri"/></font>
                    <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
                  </fonts>
                  <fills count="3">
                    <fill><patternFill patternType="none"/></fill>
                    <fill><patternFill patternType="gray125"/></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FF1E3A8A"/><bgColor indexed="64"/></patternFill></fill>
                  </fills>
                  <borders count="2">
                    <border><left/><right/><top/><bottom/><diagonal/></border>
                    <border>
                      <left style="thin"><color rgb="FFD1D5DB"/></left>
                      <right style="thin"><color rgb="FFD1D5DB"/></right>
                      <top style="thin"><color rgb="FFD1D5DB"/></top>
                      <bottom style="thin"><color rgb="FFD1D5DB"/></bottom>
                      <diagonal/>
                    </border>
                  </borders>
                  <cellStyleXfs count="1">
                    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
                  </cellStyleXfs>
                  <cellXfs count="3">
                    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
                    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
                      <alignment horizontal="center" vertical="center" wrapText="1"/>
                    </xf>
                    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1">
                      <alignment vertical="top" wrapText="1"/>
                    </xf>
                  </cellXfs>
                </styleSheet>
                """;
    }

    private String buildSheet1Xml(List<UpskillingCandidateDto> candidates) {
        StringBuilder sb = new StringBuilder(4096);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n");

        // Column widths
        sb.append("  <cols>\n");
        for (int i = 0; i < COL_WIDTHS.length; i++) {
            int colIdx = i + 1;
            sb.append("    <col min=\"").append(colIdx)
                    .append("\" max=\"").append(colIdx)
                    .append("\" width=\"").append(COL_WIDTHS[i])
                    .append("\" customWidth=\"1\"/>\n");
        }
        sb.append("  </cols>\n");

        sb.append("  <sheetData>\n");

        // Header row (row 1)
        sb.append("    <row r=\"1\" ht=\"28\" customHeight=\"1\">\n");
        for (int c = 0; c < HEADERS.length; c++) {
            appendInlineStringCell(sb, 1, c, HEADERS[c], 1);
        }
        sb.append("    </row>\n");

        // Data rows
        if (candidates != null) {
            for (int i = 0; i < candidates.size(); i++) {
                int rowNum = i + 2;
                UpskillingCandidateDto dto = candidates.get(i);
                sb.append("    <row r=\"").append(rowNum).append("\">\n");

                appendNumericCell(sb, rowNum, 0, String.valueOf(i + 1), 2);
                appendInlineStringCell(sb, rowNum, 1, dto.getCandidate() != null ? dto.getCandidate().getFullName() : "", 2);
                appendInlineStringCell(sb, rowNum, 2, dto.getCandidate() != null ? dto.getCandidate().getEmail() : "", 2);
                appendInlineStringCell(sb, rowNum, 3, dto.getCandidate() != null ? dto.getCandidate().getPhone() : "", 2);
                appendInlineStringCell(sb, rowNum, 4, dto.getClassCode(), 2);
                appendInlineStringCell(sb, rowNum, 5, dto.getCourseName(), 2);
                appendInlineStringCell(sb, rowNum, 6, dto.getTarget(), 2);
                appendInlineStringCell(sb, rowNum, 7, formatMissingSkills(dto.getMissingSkills()), 2);
                appendInlineStringCell(sb, rowNum, 8, formatEnrollments(dto.getEnrollments()), 2);
                appendNumericCell(sb, rowNum, 9, dto.getReadiness() != null ? String.valueOf(dto.getReadiness()) : "0", 2);
                appendInlineStringCell(sb, rowNum, 10, translateStatus(dto.getStatus()), 2);

                sb.append("    </row>\n");
            }
        }

        sb.append("  </sheetData>\n");
        sb.append("</worksheet>");
        return sb.toString();
    }

    private String formatMissingSkills(List<MissingSkillDto> skills) {
        if (skills == null || skills.isEmpty()) {
            return "Không có";
        }
        return skills.stream()
                .map(s -> s.getSkillName() + " (thiếu " + s.getGap() + "đ)")
                .collect(Collectors.joining("; "));
    }

    private String formatEnrollments(List<EnrollmentProgressDto> enrollments) {
        if (enrollments == null || enrollments.isEmpty()) {
            return "Chưa ghi danh";
        }
        return enrollments.stream()
                .map(e -> {
                    String prog = e.getProgress() != null ? e.getProgress().stripTrailingZeros().toPlainString() + "%" : "0%";
                    String score = e.getScore() != null ? ", điểm " + e.getScore().stripTrailingZeros().toPlainString() : "";
                    return e.getCourse() + " (" + prog + score + " - " + translateStatus(e.getStatus()) + ")";
                })
                .collect(Collectors.joining("\n"));
    }

    private String translateStatus(String status) {
        if (status == null) {
            return "";
        }
        return switch (status.toUpperCase()) {
            case "IN_PROGRESS" -> "Đang học";
            case "COMPLETED" -> "Đã hoàn thành";
            case "ENROLLED" -> "Đã ghi danh";
            case "FAILED" -> "Chưa đạt";
            case "NOT_STARTED" -> "Chưa bắt đầu";
            default -> status;
        };
    }

    private void appendInlineStringCell(StringBuilder sb, int rowNum, int colIdx, String value, int styleIdx) {
        String cellRef = toColumnName(colIdx) + rowNum;
        String safe = escapeXml(value);
        sb.append("      <c r=\"").append(cellRef).append("\" s=\"").append(styleIdx)
                .append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                .append(safe)
                .append("</t></is></c>\n");
    }

    private void appendNumericCell(StringBuilder sb, int rowNum, int colIdx, String numStr, int styleIdx) {
        String cellRef = toColumnName(colIdx) + rowNum;
        sb.append("      <c r=\"").append(cellRef).append("\" s=\"").append(styleIdx)
                .append("\" t=\"n\"><v>").append(numStr).append("</v></c>\n");
    }

    private String toColumnName(int colIdx) {
        StringBuilder name = new StringBuilder();
        int idx = colIdx;
        while (idx >= 0) {
            name.insert(0, (char) ('A' + (idx % 26)));
            idx = (idx / 26) - 1;
        }
        return name.toString();
    }

    private String escapeXml(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c < 0x20 && c != '\t' && c != '\n' && c != '\r') {
                continue;
            }
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&apos;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
