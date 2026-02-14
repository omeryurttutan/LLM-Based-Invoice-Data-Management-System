import { Role } from "./auth";

// Re-export User from auth or define specific user management types here
export interface UserListItem {
    id: string;
    email: string;
    fullName: string;
    role: Role;
    isActive: boolean;
}
