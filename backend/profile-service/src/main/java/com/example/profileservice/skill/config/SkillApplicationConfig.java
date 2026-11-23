package com.example.profileservice.skill.config;

import com.example.profileservice.service.EvidenceDecayCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SkillApplicationConfig {

    @Bean
    public EvidenceDecayCalculator evidenceDecayCalculator() {
        return new EvidenceDecayCalculator();
    }
}

