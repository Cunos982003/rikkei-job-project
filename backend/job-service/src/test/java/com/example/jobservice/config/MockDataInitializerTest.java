package com.example.jobservice.config;

import com.example.jobservice.auth.client.AuthClient;
import com.example.jobservice.company.domain.CompanyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MockDataInitializerTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private ObjectProvider<AuthClient> authClientProvider;

    @Mock
    private Environment environment;

    @Test
    @DisplayName("Bảng companies không rỗng (count > 0) → bỏ qua nạp mock data")
    void skipWhenCompaniesNotEmpty() throws Exception {
        when(companyRepository.count()).thenReturn(5L);

        MockDataInitializer initializer = new MockDataInitializer(
                companyRepository, authClientProvider, environment, "../non-existent"
        );

        initializer.run(new DefaultApplicationArguments());

        verify(companyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Thư mục mock không tồn tại ở profile test → ném IllegalStateException")
    void nonExistentDirectoryInTestProfile_throwsException() {
        when(companyRepository.count()).thenReturn(0L);
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);

        MockDataInitializer initializer = new MockDataInitializer(
                companyRepository, authClientProvider, environment, "non-existent-directory-xyz-123"
        );

        assertThatThrownBy(() -> initializer.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Mock data directory does not exist");
    }

    @Test
    @DisplayName("Thư mục mock không tồn tại ở profile local → ghi warning, không crash")
    void nonExistentDirectoryInLocalProfile_doesNotCrash() {
        when(companyRepository.count()).thenReturn(0L);
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(false);

        MockDataInitializer initializer = new MockDataInitializer(
                companyRepository, authClientProvider, environment, "non-existent-directory-xyz-123"
        );

        assertThatCode(() -> initializer.run(new DefaultApplicationArguments()))
                .doesNotThrowAnyException();
    }
}
