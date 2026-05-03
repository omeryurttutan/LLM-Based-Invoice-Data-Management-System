import { UserRole } from "./auth";

export interface UserProfileResponse {
    id: string;
    email: string;
    fullName: string;
    phone?: string;
    role: UserRole;
    companyId: string;
    companyName: string;
    lastLoginAt?: string;
    createdAt: string;
    updatedAt: string;
}

export interface UpdateProfileRequest {
    fullName: string;
    phone?: string;
}

export interface ChangePasswordRequest {
    currentPassword?: string;
    newPassword?: string;
}
