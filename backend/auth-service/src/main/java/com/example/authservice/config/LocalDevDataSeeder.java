package com.example.authservice.config;

import com.example.authservice.entity.User;
import com.example.authservice.entity.UserRole;
import com.example.authservice.entity.UserStatus;
import com.example.authservice.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@Profile("!prod")
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

        // 1. Cán bộ tuyển sinh / quan hệ doanh nghiệp (STAFF)
        User staff = new User(
                null,
                "staff1@rikkeijobs.test",
                "Hoàng Thu Hà",
                UserRole.STAFF,
                encodedPassword,
                now,
                now);
        userRepository.save(staff);

        // 2. Quản trị hệ thống (ADMIN)
        User admin = new User(
                null,
                "admin1@rikkeijobs.test",
                "Quản trị viên Hệ thống",
                UserRole.ADMIN,
                encodedPassword,
                now,
                now);
        userRepository.save(admin);

        // 3. Tài khoản HR 1 (Công ty Cổ phần Rikkeisoft)
        User hr1 = new User(
                null,
                "hr1@rikkeijobs.test",
                "HR Account One",
                UserRole.HR,
                encodedPassword,
                now,
                now);
        hr1.setCompanyId(UUID.fromString("11111111-1111-4111-8111-111111111111"));
        userRepository.save(hr1);

        // 4. Tài khoản HR 2 (Rikkei Digital Solutions)
        User hr2 = new User(
                null,
                "hr2@rikkeijobs.test",
                "HR Account Two",
                UserRole.HR,
                encodedPassword,
                now,
                now);
        hr2.setCompanyId(UUID.fromString("22222222-2222-4222-8222-222222222222"));
        userRepository.save(hr2);

        // 5. Cố vấn học tập (MENTOR)
        User mentor = new User(
                null,
                "mentor1@rikkeijobs.test",
                "Phạm Minh Đức",
                UserRole.MENTOR,
                encodedPassword,
                now,
                now);
        userRepository.save(mentor);

        // 6. Tài khoản Ứng viên 1 (CANDIDATE)
        User candidate1 = new User(
                "std-001",
                "candidate1@rikkeijobs.test",
                "Nguyễn Văn An",
                UserRole.CANDIDATE,
                encodedPassword,
                now,
                now);
        userRepository.save(candidate1);

        // 7. Tài khoản Ứng viên 2 (CANDIDATE)
        User candidate2 = new User(
                "std-002",
                "candidate2@rikkeijobs.test",
                "Trần Thị Bình",
                UserRole.CANDIDATE,
                encodedPassword,
                now,
                now);
        userRepository.save(candidate2);

        // 8. Tài khoản Ứng viên 3 (CANDIDATE)
        User candidate3 = new User(
                "std-003",
                "candidate3@rikkeijobs.test",
                "Lê Hoàng Cường",
                UserRole.CANDIDATE,
                encodedPassword,
                now,
                now);
        userRepository.save(candidate3);
    }
}
