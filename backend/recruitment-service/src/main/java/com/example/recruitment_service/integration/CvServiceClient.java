package com.example.recruitment_service.integration;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.example.recruitment_service.exception.ApiException;

@Component
public class CvServiceClient {
    private static final byte[] PDF_SIGNATURE = {'%', 'P', 'D', 'F', '-'};

    private final RestClient restClient;
    private final String cvPdfPath;
    private final int maxPdfBytes;

    public CvServiceClient(
            @Value("${app.integrations.profile-service-url}") String baseUrl,
            @Value("${app.integrations.cv-pdf-path:/api/v1/cvs/{cvId}/pdf}") String cvPdfPath,
            @Value("${app.cv.max-pdf-bytes:10485760}") int maxPdfBytes) {
        if (maxPdfBytes < PDF_SIGNATURE.length || maxPdfBytes == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("app.cv.max-pdf-bytes must be a positive, bounded size");
        }
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.cvPdfPath = cvPdfPath;
        this.maxPdfBytes = maxPdfBytes;
    }

    public byte[] downloadPdf(UUID cvId, String authorization) {
        byte[] pdf = restClient.get()
                .uri(cvPdfPath, cvId)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .accept(MediaType.APPLICATION_PDF)
                .exchange((request, response) -> {
                    if (response.getStatusCode().value() == 404) {
                        throw new ApiException(
                                org.springframework.http.HttpStatus.NOT_FOUND, "CV not found or unavailable");
                    }
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        throw new RestClientException(
                                "Profile service returned HTTP " + response.getStatusCode().value());
                    }
                    if (response.getHeaders().getContentLength() > maxPdfBytes) {
                        throw new ApiException(
                                HttpStatus.UNPROCESSABLE_ENTITY,
                                "The selected CV exceeds the PDF size limit");
                    }
                    try (InputStream input = response.getBody()) {
                        byte[] bytes = input.readNBytes(maxPdfBytes + 1);
                        if (bytes.length > maxPdfBytes) {
                            throw new ApiException(
                                    HttpStatus.UNPROCESSABLE_ENTITY,
                                    "The selected CV exceeds the PDF size limit");
                        }
                        return bytes;
                    } catch (IOException exception) {
                        throw new RestClientException("Unable to read the CV PDF response", exception);
                    }
                });

        if (pdf == null || pdf.length < PDF_SIGNATURE.length || pdf.length > maxPdfBytes
                || !hasPdfSignature(pdf)) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "The selected CV is missing or is not a valid PDF");
        }
        return pdf;
    }

    private boolean hasPdfSignature(byte[] bytes) {
        for (int i = 0; i < PDF_SIGNATURE.length; i++) {
            if (bytes[i] != PDF_SIGNATURE[i]) {
                return false;
            }
        }
        return true;
    }
}
