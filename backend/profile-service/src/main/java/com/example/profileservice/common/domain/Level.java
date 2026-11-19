package com.example.profileservice.common.domain;

public enum Level {
    CO_BAN(0, 49, "Cơ bản"),
    KHA(50, 69, "Khá"),
    VUNG(70, 84, "Vững"),
    NANG_CAO(85, 100, "Nâng cao");

    private final int min;
    private final int max;
    private final String vietnameseName;

    Level(int min, int max, String vietnameseName) {
        this.min = min;
        this.max = max;
        this.vietnameseName = vietnameseName;
    }

    public static Level fromScore(Score score) {
        return fromScore(score != null ? score.value() : null);
    }

    public static Level fromScore(Short score) {
        if (score == null) return CO_BAN;
        int s = score;
        if (s >= NANG_CAO.min) return NANG_CAO;
        if (s >= VUNG.min) return VUNG;
        if (s >= KHA.min) return KHA;
        return CO_BAN;
    }

    public String label() {
        return vietnameseName;
    }

    public int min() {
        return min;
    }

    public int max() {
        return max;
    }
}