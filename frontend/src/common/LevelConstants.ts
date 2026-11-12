/**
 * Shared level constants to ensure consistency between frontend, backend, and tests.
 * Uses NFC normalization for Vietnamese strings.
 */
export const LevelConstants = {
  LEVEL_CO_BAN: "Cơ bản",
  LEVEL_KHA: "Khá",
  LEVEL_VUNG: "Vững",
  LEVEL_NANG_CAO: "Năng cao",
} as const;

export type Level = typeof LevelConstants[keyof typeof LevelConstants];