package com.example.jobservice.company.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body của PUT /companies/me.
 * Cố tình KHÔNG có taxCode và status: HR không được sửa hai trường này.
 * Jackson mặc định bỏ qua field lạ, nên nếu client gửi kèm taxCode/status
 * thì chúng bị lờ đi, không ảnh hưởng dữ liệu.
 *
 * Trường nào null thì giữ nguyên giá trị cũ.
 */
public record UpdateCompanyRequest(
                @Pattern(regexp = ".*\\S.*", message = "Tên công ty không được để trống") @Size(max = 255, message = "Tên công ty tối đa 255 ký tự") String name,

                @Size(max = 255, message = "Ngành nghề tối đa 255 ký tự") String industry,

                // Dấu + phải escape, nếu không "1000+" sẽ không khớp
                @Pattern(regexp = "^(1-50|51-200|201-1000|1000\\+)$", message = "Quy mô công ty phải là một trong các giá trị: 1-50, 51-200, 201-1000, 1000+") String size,

                @Size(max = 500, message = "Địa chỉ tối đa 500 ký tự") String address,

                @Size(max = 100, message = "Thành phố tối đa 100 ký tự") String city,

                @Size(max = 255, message = "Website tối đa 255 ký tự") String website,

                String description) {
}