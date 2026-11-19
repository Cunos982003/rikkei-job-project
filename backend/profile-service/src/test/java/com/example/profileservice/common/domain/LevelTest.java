package com.example.profileservice.common.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class LevelTest {

    @Test
    void fromScore_null_returnsCoBan() {
        assertThat(Level.fromScore((Short) null)).isEqualTo(Level.CO_BAN);
    }

    @Test
    void fromScore_zero_returnsCoBan() {
        assertThat(Level.fromScore(Score.of((short) 0))).isEqualTo(Level.CO_BAN);
    }

    @Test
    void fromScore_49_returnsCoBan() {
        assertThat(Level.fromScore(Score.of((short) 49))).isEqualTo(Level.CO_BAN);
    }

    @Test
    void fromScore_50_returnsKha() {
        assertThat(Level.fromScore(Score.of((short) 50))).isEqualTo(Level.KHA);
    }

    @Test
    void fromScore_69_returnsKha() {
        assertThat(Level.fromScore(Score.of((short) 69))).isEqualTo(Level.KHA);
    }

    @Test
    void fromScore_70_returnsVung() {
        assertThat(Level.fromScore(Score.of((short) 70))).isEqualTo(Level.VUNG);
    }

    @Test
    void fromScore_84_returnsVung() {
        assertThat(Level.fromScore(Score.of((short) 84))).isEqualTo(Level.VUNG);
    }

    @Test
    void fromScore_85_returnsNangCao() {
        assertThat(Level.fromScore(Score.of((short) 85))).isEqualTo(Level.NANG_CAO);
    }

    @Test
    void fromScore_100_returnsNangCao() {
        assertThat(Level.fromScore(Score.of((short) 100))).isEqualTo(Level.NANG_CAO);
    }

    @ParameterizedTest
    @ValueSource(shorts = {0, 25, 49})
    void fromScore_coBanRange_returnsCoBan(short score) {
        assertThat(Level.fromScore(Score.of(score))).isEqualTo(Level.CO_BAN);
    }

    @ParameterizedTest
    @ValueSource(shorts = {50, 60, 69})
    void fromScore_khaRange_returnsKha(short score) {
        assertThat(Level.fromScore(Score.of(score))).isEqualTo(Level.KHA);
    }

    @ParameterizedTest
    @ValueSource(shorts = {70, 75, 84})
    void fromScore_vungRange_returnsVung(short score) {
        assertThat(Level.fromScore(Score.of(score))).isEqualTo(Level.VUNG);
    }

    @ParameterizedTest
    @ValueSource(shorts = {85, 90, 100})
    void fromScore_nangCaoRange_returnsNangCao(short score) {
        assertThat(Level.fromScore(Score.of(score))).isEqualTo(Level.NANG_CAO);
    }

    @Test
    void name_returnsVietnameseLabel() {
        assertThat(Level.CO_BAN.name()).isEqualTo("Cơ bản");
        assertThat(Level.KHA.name()).isEqualTo("Khá");
        assertThat(Level.VUNG.name()).isEqualTo("Vững");
        assertThat(Level.NANG_CAO.name()).isEqualTo("Nâng cao");
    }
}