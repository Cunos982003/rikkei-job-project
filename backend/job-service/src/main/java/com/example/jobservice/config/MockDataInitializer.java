package com.example.jobservice.config;

import com.example.jobservice.auth.client.AuthClient;
import com.example.jobservice.auth.client.StubAuthClient;
import com.example.jobservice.company.domain.Company;
import com.example.jobservice.company.domain.CompanyRepository;
import com.example.jobservice.company.domain.CompanyStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Profile({"local", "test"})
public class MockDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MockDataInitializer.class);

    private final CompanyRepository companyRepository;
    private final ObjectProvider<AuthClient> authClientProvider;
    private final Environment environment;
    private final String mockDataDir;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MockDataInitializer(
            CompanyRepository companyRepository,
            ObjectProvider<AuthClient> authClientProvider,
            Environment environment,
            @Value("${mock.data.dir:../mock/data}") String mockDataDir) {
        this.companyRepository = companyRepository;
        this.authClientProvider = authClientProvider;
        this.environment = environment;
        this.mockDataDir = mockDataDir;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (companyRepository.count() > 0) {
            log.info("Companies table is not empty, skipping mock data initialization");
            initStubAccountsIfAvailable();
            return;
        }

        Path dataDirectory = resolveDataDirectory(mockDataDir);
        if (dataDirectory == null || !Files.isDirectory(dataDirectory)) {
            String message = "Mock data directory does not exist or is not a directory: "
                    + (dataDirectory != null ? dataDirectory : mockDataDir);
            handleMissingResource(message);
            return;
        }

        Path companiesFile = dataDirectory.resolve("companies.json");
        if (!Files.isRegularFile(companiesFile)) {
            handleMissingResource("Mock data file does not exist: " + companiesFile);
            return;
        }

        try {
            Map<String, UUID> companyIds = loadCompanies(companiesFile);
            AuthClient authClient = authClientProvider.getIfAvailable();
            if (authClient instanceof StubAuthClient stub) {
                stub.initDefaultAccounts(companyIds.get("comp-001"), companyIds.get("comp-002"));
            }
            log.info("Successfully initialized mock data from {}", dataDirectory.toAbsolutePath());
        } catch (Exception e) {
            if (isTestProfile()) {
                throw e;
            } else {
                log.warn("Failed to load mock data from {}: {}", dataDirectory, e.getMessage(), e);
            }
        }
    }

    private void initStubAccountsIfAvailable() {
        AuthClient authClient = authClientProvider.getIfAvailable();
        if (authClient instanceof StubAuthClient stub && stub.getAllAccounts().isEmpty()) {
            companyRepository.findByTaxCode("0101234567").ifPresent(c1 ->
                companyRepository.findByTaxCode("0107654321").ifPresent(c2 ->
                    stub.initDefaultAccounts(c1.getId(), c2.getId())
                )
            );
        }
    }

    private void handleMissingResource(String message) {
        if (isTestProfile()) {
            throw new IllegalStateException(message);
        } else {
            log.warn(message);
        }
    }

    private boolean isTestProfile() {
        return environment.acceptsProfiles(Profiles.of("test"));
    }

    private Path resolveDataDirectory(String dirPath) {
        if (dirPath == null || dirPath.isBlank()) {
            return null;
        }
        Path path = Path.of(dirPath);
        if (Files.isDirectory(path)) {
            return path;
        }
        Path altPath = Path.of("..", dirPath);
        if (Files.isDirectory(altPath)) {
            return altPath;
        }
        Path projectRootMock = Path.of("../..", "mock", "data");
        if (Files.isDirectory(projectRootMock)) {
            return projectRootMock;
        }
        Path currentMock = Path.of("mock", "data");
        if (Files.isDirectory(currentMock)) {
            return currentMock;
        }
        return path;
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
