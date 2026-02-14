"use client"

import { useAuthStore } from "@/stores/auth-store"
import { Role } from "@/types/auth"

interface RoleGateProps {
  children: React.ReactNode
  allowedRoles: Role[]
  fallback?: React.ReactNode
}

export function RoleGate({ children, allowedRoles, fallback = null }: RoleGateProps) {
  const user = useAuthStore((state) => state.user)

  if (!user || !allowedRoles.includes(user.role)) {
    return fallback
  }

  return <>{children}</>
}
