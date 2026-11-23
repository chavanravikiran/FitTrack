import { RoleName } from './auth.models';

export type Gender = 'MALE' | 'FEMALE' | 'PREFER_NOT_TO_SAY';
export type ThemePreference = 'LIGHT' | 'DARK' | 'SYSTEM';

export interface Profile {
  userId: number;
  email: string;
  fullName: string;
  dateOfBirth: string | null;
  age: number | null;
  gender: Gender | null;
  heightCm: number | null;
  timeZone: string;
  theme: ThemePreference;
  hasPhoto: boolean;
  onboardingCompleted: boolean;
  roles: RoleName[];
  currentWeightKg: number | null;
  targetWeightKg: number | null;
  bmi: number | null;
  memberSince: string;
}

export interface UpdateProfileRequest {
  fullName: string;
  dateOfBirth: string;
  gender: Gender;
  heightCm: number;
  timeZone: string;
  theme: ThemePreference;
}
