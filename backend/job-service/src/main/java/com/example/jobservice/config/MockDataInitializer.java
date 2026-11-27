package com.example.jobservice.config;

import com.example.jobservice.auth.client.AuthClient;
import com.example.jobservice.auth.client.StubAuthClient;
import com.example.jobservice.company.domain.Company;
import com.example.jobservice.company.domain.CompanyRepository;
import com.example.jobservice.company.domain.CompanyStatus;
import com.example.jobservice.job.catalog.CatalogSkill;
import com.example.jobservice.job.catalog.SkillCatalogClient;
import com.example.jobservice.job.domain.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;

@Component
@Profile({ "local", "test" })
public class MockDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MockDataInitializer.class);

    private final CompanyRepository companyRepository;
    private final ObjectProvider<AuthClient> authClientProvider;
    private final ObjectProvider<JobRepository> jobRepositoryProvider;
    private final ObjectProvider<JobSkillRepository> jobSkillRepositoryProvider;
    private final ObjectProvider<SkillCatalogClient> skillCatalogClientProvider;
    private final ObjectProvider<Clock> clockProvider;
    private final Environment environment;
    private final String mockDataDir;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MockDataInitializer(
            CompanyRepository companyRepository,
            ObjectProvider<AuthClient> authClientProvider,
            Environment environment,
            @Value("${mock.data.dir:../mock/data}") String mockDataDir) {
        this(companyRepository, authClientProvider, null, null, null, null, environment, mockDataDir);
    }

    @Autowired
    public MockDataInitializer(
            CompanyRepository companyRepository,
            ObjectProvider<AuthClient> authClientProvider,
            ObjectProvider<JobRepository> jobRepositoryProvider,
            ObjectProvider<JobSkillRepository> jobSkillRepositoryProvider,
            ObjectProvider<SkillCatalogClient> skillCatalogClientProvider,
            ObjectProvider<Clock> clockProvider,
            Environment environment,
            @Value("${mock.data.dir:../mock/data}") String mockDataDir) {
        this.companyRepository = companyRepository;
        this.authClientProvider = authClientProvider;
        this.jobRepositoryProvider = jobRepositoryProvider;
        this.jobSkillRepositoryProvider = jobSkillRepositoryProvider;
        this.skillCatalogClientProvider = skillCatalogClientProvider;
        this.clockProvider = clockProvider;
        this.environment = environment;
        this.mockDataDir = mockDataDir;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (companyRepository.count() > 0) {
            log.info("Companies table is not empty, skipping mock data initialization");
            initStubAccountsIfAvailable();
            ensureSamplePendingJob();
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

            Path jobsFile = dataDirectory.resolve("jobs.json");
            if (Files.isRegularFile(jobsFile)) {
                loadJobs(jobsFile, companyIds);
            }

            ensureSamplePendingJob();

            log.info("Successfully initialized mock data from {}", dataDirectory.toAbsolutePath());
        } catch (Exception e) {
            if (isTestProfile()) {
                throw e;
            } else {
                log.warn("Failed to load mock data from {}: {}", dataDirectory, e.getMessage(), e);
            }
        }
    }

    private void loadJobs(Path jobsFile, Map<String, UUID> companyIds) throws IOException {
        JobRepository jobRepository = jobRepositoryProvider != null ? jobRepositoryProvider.getIfAvailable() : null;
        JobSkillRepository jobSkillRepository = jobSkillRepositoryProvider != null ? jobSkillRepositoryProvider.getIfAvailable() : null;
        SkillCatalogClient skillCatalogClient = skillCatalogClientProvider != null ? skillCatalogClientProvider.getIfAvailable() : null;
        Clock clock = clockProvider != null ? clockProvider.getIfAvailable() : null;

        if (jobRepository == null || jobSkillRepository == null) {
            log.info("Job repositories not available, skipping job mock data loading");
            return;
        }

        LocalDate today = clock != null ? LocalDate.now(clock) : LocalDate.now();
        LocalDate defaultDeadline = today.plusDays(30);

        JsonNode jobsArray = readArray(jobsFile);
        JobStatus[] sequentialStatuses = { JobStatus.DRAFT, JobStatus.PENDING, JobStatus.APPROVED };

        int index = 0;
        for (JsonNode data : jobsArray) {
            String mockCompanyId = optionalText(data, "companyId");
            UUID companyId = mockCompanyId != null ? companyIds.get(mockCompanyId) : null;
            if (companyId == null) {
                log.warn("Company not found for mock companyId: {}", mockCompanyId);
                continue;
            }

            String title = requiredText(data, "title", jobsFile);
            String position = optionalText(data, "position");
            if (position == null || position.isBlank()) {
                position = title;
            }

            JobLevel level = JobLevel.JUNIOR;
            String levelStr = optionalText(data, "level");
            if (levelStr != null) {
                try {
                    level = JobLevel.valueOf(levelStr.trim().toUpperCase());
                } catch (IllegalArgumentException ignored) {}
            }

            JobType type = JobType.FULLTIME;
            String jobTypeStr = optionalText(data, "jobType");
            if (jobTypeStr != null) {
                String normalized = jobTypeStr.trim().toUpperCase().replace("_", "");
                if ("INTERNSHIP".equals(normalized)) type = JobType.INTERNSHIP;
                else if ("PARTTIME".equals(normalized)) type = JobType.PARTTIME;
                else type = JobType.FULLTIME;
            }

            String city = optionalText(data, "city");
            if (city == null) {
                city = optionalText(data, "location");
            }

            WorkMode workMode = WorkMode.ONSITE;
            String workModeStr = optionalText(data, "workMode");
            if (workModeStr != null) {
                try {
                    workMode = WorkMode.valueOf(workModeStr.trim().toUpperCase());
                } catch (IllegalArgumentException ignored) {}
            }

            BigDecimal salaryMin = null;
            if (data.hasNonNull("salaryMin")) {
                salaryMin = BigDecimal.valueOf(data.get("salaryMin").asDouble());
            }

            BigDecimal salaryMax = null;
            if (data.hasNonNull("salaryMax")) {
                salaryMax = BigDecimal.valueOf(data.get("salaryMax").asDouble());
            }

            JobStatus status = index < sequentialStatuses.length ? sequentialStatuses[index] : JobStatus.APPROVED;
            index++;

            Job job = new Job();
            job.setCompanyId(companyId);
            job.setTitle(title);
            job.setPosition(position);
            job.setLevel(level);
            job.setType(type);
            job.setQuantity(1);
            job.setCity(city);
            job.setAddress(optionalText(data, "address"));
            job.setWorkMode(workMode);
            job.setSalaryMin(salaryMin);
            job.setSalaryMax(salaryMax);
            job.setDescription(optionalText(data, "description"));
            job.setRequirements(optionalText(data, "requirements"));
            job.setBenefits(optionalText(data, "benefits"));
            job.setDeadline(defaultDeadline);
            job.setStatus(status);
            job.setCreatedBy("system-mock");

            Job savedJob = jobRepository.save(job);

            JsonNode skillsNode = data.get("jobSkills");
            if (skillsNode != null && skillsNode.isArray() && skillCatalogClient != null) {
                List<JobSkill> jobSkills = new ArrayList<>();
                for (JsonNode skillItem : skillsNode) {
                    String skillCodeOrId = optionalText(skillItem, "skillId");
                    if (skillCodeOrId == null || skillCodeOrId.isBlank()) {
                        continue;
                    }

                    Optional<CatalogSkill> catalogSkillOpt = skillCatalogClient.findByIdOrCode(skillCodeOrId.trim());
                    if (catalogSkillOpt.isEmpty()) {
                        log.warn("Skill not found in catalog for code: {}", skillCodeOrId);
                        continue;
                    }

                    CatalogSkill catalogSkill = catalogSkillOpt.get();
                    boolean required = true;
                    if (skillItem.has("isMandatory")) {
                        required = skillItem.get("isMandatory").asBoolean(true);
                    } else if (skillItem.has("required")) {
                        required = skillItem.get("required").asBoolean(true);
                    }
                    int minScore = skillItem.hasNonNull("minScore") ? skillItem.get("minScore").asInt(0) : 0;

                    jobSkills.add(new JobSkill(
                            savedJob.getId(),
                            catalogSkill.id(),
                            catalogSkill.code(),
                            catalogSkill.name(),
                            required,
                            minScore
                    ));

                }
                if (!jobSkills.isEmpty()) {
                    jobSkillRepository.saveAll(jobSkills);
                }
            }
        }
    }

    private void ensureSamplePendingJob() {
        JobRepository jobRepository = jobRepositoryProvider != null ? jobRepositoryProvider.getIfAvailable() : null;
        if (jobRepository == null) {
            return;
        }

        Optional<Company> sampleCompanyOpt = companyRepository.findByTaxCode("0101234567");
        if (sampleCompanyOpt.isEmpty()) {
            sampleCompanyOpt = companyRepository.findAll().stream().findFirst();
        }
        if (sampleCompanyOpt.isEmpty()) {
            return;
        }

        Company company = sampleCompanyOpt.get();
        String sampleTitle = "[MẪU] Chuyên viên phát triển Java (Chờ duyệt)";

        org.springframework.data.domain.Page<Job> existingJobs = jobRepository.findByCompanyId(
                company.getId(),
                org.springframework.data.domain.PageRequest.of(0, 100));
        boolean exists = existingJobs.getContent().stream()
                .anyMatch(j -> sampleTitle.equalsIgnoreCase(j.getTitle()));
        if (exists) {
            return;
        }

        Clock clock = clockProvider != null ? clockProvider.getIfAvailable() : null;
        LocalDate today = clock != null ? LocalDate.now(clock) : LocalDate.now();

        Job pendingJob = new Job();
        pendingJob.setCompanyId(company.getId());
        pendingJob.setTitle(sampleTitle);
        pendingJob.setPosition("Backend Java Developer");
        pendingJob.setLevel(JobLevel.MIDDLE);
        pendingJob.setType(JobType.FULLTIME);
        pendingJob.setQuantity(2);
        pendingJob.setCity(company.getCity() != null ? company.getCity() : "Hà Nội");
        pendingJob.setAddress(company.getAddress() != null ? company.getAddress() : "Keangnam Hanoi Landmark Tower");
        pendingJob.setWorkMode(WorkMode.HYBRID);
        pendingJob.setSalaryMin(BigDecimal.valueOf(15000000));
        pendingJob.setSalaryMax(BigDecimal.valueOf(25000000));
        pendingJob.setDescription("Phát triển các module nghiệp vụ tuyển dụng và khớp lệnh.");
        pendingJob.setRequirements("Ít nhất 2 năm kinh nghiệm Spring Boot, PostgreSQL, Docker.");
        pendingJob.setBenefits("Lương tháng 13, BHXH đầy đủ, xét tăng lương định kỳ.");
        pendingJob.setDeadline(today.plusDays(30));
        pendingJob.setStatus(JobStatus.PENDING);
        pendingJob.setCreatedBy("hr-sample");

        Job saved = jobRepository.save(pendingJob);

        JobSkillRepository jobSkillRepository = jobSkillRepositoryProvider != null ? jobSkillRepositoryProvider.getIfAvailable() : null;
        if (jobSkillRepository != null) {
            List<JobSkill> skills = List.of(
                    new JobSkill(saved.getId(), "JAVA_CORE", "JAVA_CORE", "Java Core", true, 70),
                    new JobSkill(saved.getId(), "SPRING_BOOT", "SPRING_BOOT", "Spring Boot", true, 60)
            );
            jobSkillRepository.saveAll(skills);
        }
        log.info("Initialized sample PENDING job for company: {}", company.getName());
    }

    private void initStubAccountsIfAvailable() {
        AuthClient authClient = authClientProvider.getIfAvailable();
        if (authClient instanceof StubAuthClient stub && stub.getAllAccounts().isEmpty()) {
            companyRepository.findByTaxCode("0101234567").ifPresent(c1 -> companyRepository.findByTaxCode("0107654321")
                    .ifPresent(c2 -> stub.initDefaultAccounts(c1.getId(), c2.getId())));
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
        // Mặc định ../mock/data tính từ backend/job-service sẽ ra ../../mock/data (gốc
        // repo)
        Path altPath = Path.of("..", dirPath);
        if (Files.isDirectory(altPath)) {
            return altPath;
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
