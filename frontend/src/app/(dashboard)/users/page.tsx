'use client';

import { Users, UserPlus } from "lucide-react"
import { Button } from "@/components/ui/button"
import { PageHeader } from "@/components/common/page-header"
import { useTranslations } from "next-intl"
import { useState, useEffect, useCallback } from "react"
import { UserResponse } from "@/types/user"
import { userService } from "@/services/user-service"
import { UsersTable } from "./_components/users-table"
import { UserFormDialog } from "./_components/user-form-dialog"
import { toast } from "sonner"

export default function UsersPage() {
  const t = useTranslations('common.pages.users');
  const [users, setUsers] = useState<UserResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<UserResponse | null>(null);

  const loadUsers = useCallback(async () => {
    try {
      setLoading(true);
      const data = await userService.getUsers();
      setUsers(data.content);
    } catch (error) {
      toast.error(t('messages.error'));
      console.error(error);
    } finally {
      setLoading(false);
    }
  }, [t]);

  useEffect(() => {
    loadUsers();
  }, [loadUsers]);

  const handleEdit = (user: UserResponse) => {
    setEditingUser(user);
    setDialogOpen(true);
  };

  const handleNew = () => {
    setEditingUser(null);
    setDialogOpen(true);
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title={t('title')}
        description={t('description')}
        actions={
          <Button onClick={handleNew}>
            <UserPlus className="mr-2 h-4 w-4" />
            {t('newUser')}
          </Button>
        }
      />
      
      {loading ? (
        <div className="flex justify-center items-center py-12">
           <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
        </div>
      ) : (
        <UsersTable 
          users={users} 
          onRefresh={loadUsers} 
          onEdit={handleEdit} 
        />
      )}

      <UserFormDialog 
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        user={editingUser}
        onSuccess={loadUsers}
      />
    </div>
  )
}
