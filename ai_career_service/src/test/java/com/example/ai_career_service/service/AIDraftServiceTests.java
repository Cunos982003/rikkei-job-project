package com.example.ai_career_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ai_career_service.dto.AIDraftRequest;
import com.example.ai_career_service.enums.AIDraftSection;
import com.example.ai_career_service.enums.AIDraftStatus;
import com.example.ai_career_service.mapper.AIDraftMapper;
import com.example.ai_career_service.model.AIDraft;
import com.example.ai_career_service.provider.AIProvider;
import com.example.ai_career_service.repository.AIDraftRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AIDraftServiceTests {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CV_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID DRAFT_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Mock
    private AIDraftRepository repository;

    @Mock
    private AIDraftDataSource dataSource;

    @Mock
    private AIDraftCvWriter cvWriter;

    @Mock
    private AIProvider aiProvider;

    private AIDraftService service;

    @BeforeEach
    void setUp() {
        service = new AIDraftService(
                repository,
                dataSource,
                cvWriter,
                aiProvider,
                new AIDraftMapper(),
                new ObjectMapper());
    }

    @Test
    void createsDraftAndWarnsAboutUnsupportedSkillsAndNumbers() throws Exception {
        var snapshot = new ObjectMapper().readTree("""
                {"verifiedSkills":["Java"],"projects":[{"description":"Built Java APIs"}]}
                """);
        when(dataSource.loadInputSnapshot(USER_ID, CV_ID, AIDraftSection.SUMMARY, null, null))
                .thenReturn(snapshot);
        when(aiProvider.generateDraft(anyString()))
                .thenReturn("""
                        {"content":"Built Java APIs with Rust and improved performance by 20% in 3 years.",
                         "skills":["Java","Rust"]}
                        """);
        when(repository.save(any(AIDraft.class))).thenAnswer(invocation -> {
            AIDraft draft = invocation.getArgument(0);
            draft.setId(DRAFT_ID);
            return draft;
        });

        var response = service.createDraft(
                USER_ID,
                CV_ID,
                new AIDraftRequest(AIDraftSection.SUMMARY, null, null));

        assertThat(response.id()).isEqualTo(DRAFT_ID);
        assertThat(response.status()).isEqualTo(AIDraftStatus.DRAFT);
        assertThat(response.warnings())
                .containsExactly(
                        "Tên kỹ năng/công nghệ không có trong dữ liệu đầu vào: Rust",
                        "Số liệu không có trong dữ liệu đầu vào: 20%",
                        "Số liệu không có trong dữ liệu đầu vào: 3 years");
    }

    @Test
    void acceptsOnlyOwnedDraftAndWritesItBeforeChangingStatus() {
        AIDraft draft = new AIDraft();
        draft.setId(DRAFT_ID);
        draft.setUserId(USER_ID);
        draft.setCvId(CV_ID);
        draft.setSection(AIDraftSection.PROJECT);
        draft.setOutput("Project description");
        draft.setStatus(AIDraftStatus.DRAFT);
        when(repository.findByIdAndUserId(DRAFT_ID, USER_ID)).thenReturn(Optional.of(draft));
        when(repository.save(draft)).thenReturn(draft);

        var response = service.accept(USER_ID, DRAFT_ID);

        verify(cvWriter).writeDraft(draft);
        assertThat(response.status()).isEqualTo(AIDraftStatus.ACCEPTED);
    }
}
