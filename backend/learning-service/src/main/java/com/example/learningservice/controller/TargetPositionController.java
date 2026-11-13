package com.example.learningservice.controller;

import com.example.learningservice.model.dto.response.ApiResponse;
import com.example.learningservice.model.dto.response.TargetPositionResponse;
import com.example.learningservice.service.TargetPositionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping({"/api/v1/learning/target-positions", "/learning/target-positions"})
@RequiredArgsConstructor
@Tag(name = "Target Positions", description = "Quản lý danh mục vị trí mục tiêu và bộ kỹ năng chuẩn hóa (DT-01)")
public class TargetPositionController {

    private final TargetPositionService targetPositionService;

    @GetMapping
    @Operation(summary = "Lấy danh sách các vị trí mục tiêu", description = "Trả về danh sách tất cả vị trí mục tiêu được định nghĩa sẵn (Java Backend, Frontend, Tester, Fullstack) phục vụ dropdown lựa chọn mục tiêu của học viên.")
    public ResponseEntity<ApiResponse<List<TargetPositionResponse>>> getAllPositions() {
        log.info("API: Truy vấn danh sách tất cả vị trí mục tiêu");
        List<TargetPositionResponse> positions = targetPositionService.getAllPositions();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách vị trí mục tiêu thành công", positions));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết vị trí mục tiêu theo ID", description = "Trả về thông tin chi tiết một vị trí mục tiêu kèm theo danh sách các kỹ năng bắt buộc và điểm sàn yêu cầu.")
    public ResponseEntity<ApiResponse<TargetPositionResponse>> getPositionById(
            @Parameter(description = "ID của vị trí mục tiêu (UUID)") @PathVariable UUID id) {
        log.info("API: Truy vấn chi tiết vị trí mục tiêu ID: {}", id);
        TargetPositionResponse position = targetPositionService.getPositionResponseById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết vị trí mục tiêu thành công", position));
    }
}

