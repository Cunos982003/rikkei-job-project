package com.example.recruitmentservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.recruitment_service.RecruitmentServiceApplication;

@SpringBootTest(classes = RecruitmentServiceApplication.class)
@ActiveProfiles("test")
class RecruitmentServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
