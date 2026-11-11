package com.example.jobservice.company.service;

import com.example.jobservice.company.dto.HrAccountResponse;

public record HrAccountCreatedEvent(
        String email,
        String fullName,
        String companyName,
        String temporaryPassword,
        HrAccountResponse response) {
}
