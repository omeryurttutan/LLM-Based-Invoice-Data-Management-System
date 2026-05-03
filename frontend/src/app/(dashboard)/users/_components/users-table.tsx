import { useState } from 'react';
import { useTranslations } from 'next-intl';
import { UserResponse } from '@/types/user';
import { userService } from '@/services/user-service';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { MoreHorizontal, Pencil, Ban, CheckCircle } from 'lucide-react';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { toast } from 'sonner';

interface UsersTableProps {
  users: UserResponse[];
  onRefresh: () => void;
  onEdit: (user: UserResponse) => void;
}

export function UsersTable({ users, onRefresh, onEdit }: UsersTableProps) {
  const t = useTranslations('common.pages.users');
  const [loadingId, setLoadingId] = useState<string | null>(null);

  const handleToggleStatus = async (user: UserResponse) => {
    try {
      setLoadingId(user.id);
      const res = await userService.toggleUserActive(user.id);
      toast.success(user.isActive ? t('messages.deactivated') : t('messages.activated'));
      onRefresh();
    } catch (error: any) {
      const errorMsg = error.response?.data?.message || t('messages.error');
      toast.error(errorMsg);
    } finally {
      setLoadingId(null);
    }
  };

  return (
    <div className="rounded-md border">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>{t('table.name')}</TableHead>
            <TableHead>{t('table.email')}</TableHead>
            <TableHead>{t('table.role')}</TableHead>
            <TableHead>{t('table.status')}</TableHead>
            <TableHead className="text-right">{t('table.actions')}</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {!users || users.length === 0 ? (
            <TableRow>
              <TableCell colSpan={5} className="text-center h-24 text-muted-foreground">
                {t('table.noUsers')}
              </TableCell>
            </TableRow>
          ) : (
            users?.map((user) => (
              <TableRow key={user.id}>
                <TableCell className="font-medium">{user.fullName}</TableCell>
                <TableCell>{user.email}</TableCell>
                <TableCell>
                  <Badge variant="outline">{user.role}</Badge>
                </TableCell>
                <TableCell>
                  <Badge variant={user.isActive ? 'default' : 'secondary'}>
                    {user.isActive ? t('status.active') : t('status.inactive')}
                  </Badge>
                </TableCell>
                <TableCell className="text-right">
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button variant="ghost" className="h-8 w-8 p-0" disabled={loadingId === user.id}>
                        <span className="sr-only">Open menu</span>
                        <MoreHorizontal className="h-4 w-4" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end">
                      <DropdownMenuLabel>{t('table.actions')}</DropdownMenuLabel>
                      <DropdownMenuItem onClick={() => onEdit(user)}>
                        <Pencil className="mr-2 h-4 w-4" />
                        {t('table.edit')}
                      </DropdownMenuItem>
                      <DropdownMenuSeparator />
                      <DropdownMenuItem onClick={() => handleToggleStatus(user)}>
                        {user.isActive ? (
                          <>
                            <Ban className="mr-2 h-4 w-4 text-destructive" />
                            <span className="text-destructive">{t('table.deactivate')}</span>
                          </>
                        ) : (
                          <>
                            <CheckCircle className="mr-2 h-4 w-4 text-green-600" />
                            <span className="text-green-600">{t('table.activate')}</span>
                          </>
                        )}
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </TableCell>
              </TableRow>
            ))
          )}
        </TableBody>
      </Table>
    </div>
  );
}
