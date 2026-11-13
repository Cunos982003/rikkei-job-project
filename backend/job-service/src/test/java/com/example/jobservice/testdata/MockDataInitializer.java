package com.example.jobservice.testdata;

import com.example.jobservice.company.domain.Company;
import com.example.jobservice.company.domain.CompanyRepository;
import com.example.jobservice.company.domain.CompanyStatus;
import com.example.jobservice.user.domain.User;
import com.example.jobservice.user.domain.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Profile("test")
public class MockDataInitializer implements ApplicationRunner {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MockDataInitializer(
            CompanyRepository companyRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (companyRepository.count() > 0) {
            return;
        }
        String mockDir = System.getProperty("mock.data.dir");
        if (mockDir == null) {
            return;
        }
        Path dataDirectory = Path.of(mockDir);
        Map<String, UUID> companyIds = loadCompanies(dataDirectory.resolve("companies.json"));
        loadUsers(dataDirectory.resolve("users.json"), companyIds);
    }

    private Map<String, UUID> loadCompanies(Path file) throws IOException {
        JsonNode companies = readArray(file);
        Map<String, UUID> companyIds = new HashMap<>();

        for (JsonNode data : companies) {
            String mockId = requiredText(data, "id", file);
            Company company = new Company();
            company.setName(requiredText(data, "name", file));
            company.setTaxCode(requiredText(data, "taxCode", file));
            company.setIndustry(optionalText(data, "industry"));
            company.setSize(optionalText(data, "size"));
            company.setAddress(optionalText(data, "address"));
            company.setCity(optionalText(data, "city"));
            company.setWebsite(optionalText(data, "website"));
            company.setLogoUrl(optionalText(data, "logoUrl"));
            company.setDescription(optionalText(data, "description"));
            company.setStatus(CompanyStatus.valueOf(requiredText(data, "status", file)));
            Company savedCompany = companyRepository.save(company);
            companyIds.put(mockId, savedCompany.getId());
        }

        return companyIds;
    }

    private void loadUsers(Path file, Map<String, UUID> companyIds) throws IOException {
        for (JsonNode data : readArray(file)) {
            User user = new User();
            requiredText(data, "id", file);
            user.setEmail(requiredText(data, "email", file));
            user.setFullName(requiredText(data, "fullName", file));
            user.setPasswordHash(passwordEncoder.encode(requiredText(data, "password", file)));
            user.setRole("ROLE_" + requiredText(data, "role", file));

            String mockCompanyId = optionalText(data, "companyId");
            if (mockCompanyId != null) {
                UUID companyId = companyIds.get(mockCompanyId);
                if (companyId == null) {
                    throw new IllegalStateException(
                            "Mock user references unknown company id '%s' in %s".formatted(mockCompanyId, file));
                }
                user.setCompanyId(companyId);
            }

            user.setMustChangePassword(false);
            userRepository.save(user);
        }
    }

    private JsonNode readArray(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException("Mock data file does not exist: " + file);
        }

        JsonNode data = objectMapper.readTree(file.toFile());
        if (data == null || !data.isArray()) {
            throw new IllegalStateException("Mock data must be a JSON array: " + file);
        }
        return data;
    }

    private String requiredText(JsonNode data, String field, Path file) {
        String value = optionalText(data, field);
        if (value == null) {
            throw new IllegalStateException("Missing required field '%s' in %s".formatted(field, file));
        }
        return value;
    }

    private String optionalText(JsonNode data, String field) {
        JsonNode value = data.get(field);
        return value == null || value.isNull() || value.asText().isBlank() ? null : value.asText();
    }
}
