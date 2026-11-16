package com.example.authservice.config;

import com.example.authservice.entity.User;
import com.example.authservice.entity.UserStatus;
import com.example.authservice.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@Profile("local")
public class LocalDevDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public LocalDevDataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        String encodedPassword = passwordEncoder.encode("Test@123");
        Instant now = Instant.now();

        // 1. Cán bộ tuyển dụng (STAFF)
        User staff = new User(
                null,
                "staff1@rikkeijobs.test",
                "Hoàng Thu Hà",
                "STAFF",
                encodedPassword,
                now,
                now);
        userRepository.save(staff);

        // 2. Quản trị hệ thống (ADMIN)
        User admin = new User(
                null,
                "admin1@rikkeijobs.test",
                "Quản trị viên Hệ thống",
                "ADMIN",
                encodedPassword,
                now,
                now);
        userRepository.save(admin);

        // 3. Tài khoản HR (Gán sẵn công ty mẫu ở job-service)
        User hr = new User(
                null,
                "hr1@rikkeijobs.test",
                "HR Account One",
                "HR",
                encodedPassword,
                now,
                now);
        hr.setCompanyId(UUID.fromString("11111111-1111-4111-8111-111111111111"));
        userRepository.save(hr);

        // 4. Tài khoản Ứng viên (CANDIDATE)
        User candidate = new User(
                "std-001",
                "candidate1@rikkeijobs.test",
                "Nguyễn Văn An",
                "CANDIDATE",
                encodedPassword,
                now,
                now);
        userRepository.save(candidate);
    }
}
