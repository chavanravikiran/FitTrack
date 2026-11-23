export type RoleName = 'ROLE_USER' | 'ROLE_ADMIN';

export interface UserSummary {
  id: number;
  email: string;
  fullName: string | null;
  roles: RoleName[];
  onboardingCompleted: boolean;
  emailVerified: boolean;
  planCode: string;
  premium: boolean;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: 'Bearer';
  expiresIn: number;
  user: UserSummary;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
  timeZone?: string;
  acceptTerms: boolean;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}
