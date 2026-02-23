import { UserRole } from "./auth";

export interface UserResponse {
    id: string;
    email: string;
    fullName: string;
    role: UserRole;
    companyId: string;
    companyName: string;
    isActive: boolean;
    createdAt: string;
    updatedAt: string;
}

export interface CreateUserRequest {
    email: string;
    fullName: string;
    role: UserRole;
    password?: string; // Optional if backend generates it or if the admin sets it
}

export interface UpdateUserRequest {
    fullName: string;
}

export interface ChangeRoleRequest {
    role: UserRole;
}
