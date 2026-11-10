import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";
import type { SkillProfileResponse } from "@/api/types/skill";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function groupSkillsByGroup(skills: SkillProfileResponse[]): Record<string, SkillProfileResponse[]> {
  return skills.reduce((acc, skill) => {
    if (!acc[skill.group]) {
      acc[skill.group] = [];
    }
    acc[skill.group].push(skill);
    return acc;
  }, {} as Record<string, SkillProfileResponse[]>);
}

export function isSelfOnly(skill: SkillProfileResponse): boolean {
  return skill.verifiedScore === null && skill.selfScore !== null;
}