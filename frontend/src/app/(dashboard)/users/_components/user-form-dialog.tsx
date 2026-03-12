import { useState, useEffect } from 'react';
import { useTranslations } from 'next-intl';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { UserResponse, CreateUserRequest, UpdateUserRequest } from '@/types/user';
import { userService } from '@/services/user-service';
import { UserRole } from '@/types/auth';
import { CheckCircle2 } from 'lucide-react';

const roles: UserRole[] = ['ADMIN', 'MANAGER', 'ACCOUNTANT', 'INTERN'];

interface UserFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  user?: UserResponse | null;
  onSuccess: () => void;
}

export function UserFormDialog({ open, onOpenChange, user, onSuccess }: UserFormDialogProps) {
  const t = useTranslations('common.pages.users');
  const tVal = useTranslations('auth.validation');
  const [isLoading, setIsLoading] = useState(false);

  const formSchema = z.object({
    email: z.string().email({ message: t('form.emailInvalid') }),
    fullName: z.string().min(2, { message: t('form.nameRequired') }),
    role: z.string().min(1, { message: t('form.roleRequired') }),
    password: z.string().optional()
      .superRefine((val, ctx) => {
        if (!user && (!val || val.length < 8)) {
          ctx.addIssue({
            code: z.ZodIssueCode.custom,
            message: tVal('requirements.length', { min: 8 }),
          });
        }
        if (val && val.length > 0 && !/^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).*$/.test(val)) {
            ctx.addIssue({
                code: z.ZodIssueCode.custom,
                message: tVal('requirements.title'),
            });
        }
      }),
  });

  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      email: user?.email || '',
      fullName: user?.fullName || '',
      role: user?.role || 'ACCOUNTANT',
      password: '',
    },
  });

  const password = form.watch('password');

  const passwordRequirements = [
    { regex: /.{8,}/, text: tVal('requirements.length', { min: 8 }) },
    { regex: /[A-Z]/, text: tVal('requirements.uppercase') },
    { regex: /[a-z]/, text: tVal('requirements.lowercase') },
    { regex: /[0-9]/, text: tVal('requirements.number') },
    { regex: /[!@#$%^&*(),.?":{}|<>]/, text: tVal('requirements.special') },
  ];

  useEffect(() => {
    if (open) {
      if (user) {
        form.reset({
          email: user.email,
          fullName: user.fullName,
          role: user.role,
          password: '',
        });
      } else {
        form.reset({
          email: '',
          fullName: '',
          role: 'ACCOUNTANT',
          password: '',
        });
      }
    }
  }, [open, user, form]);

  const onSubmit = async (values: z.infer<typeof formSchema>) => {
    try {
      setIsLoading(true);

      if (user) {
        await userService.updateUser(user.id, {
          fullName: values.fullName,
        });
        
        if (values.role !== user.role) {
            await userService.changeUserRole(user.id, { role: values.role as UserRole });
        }
        toast.success(t('messages.updateSuccess'));
      } else {
        await userService.createUser({
          email: values.email,
          fullName: values.fullName,
          role: values.role as UserRole,
          password: values.password || undefined,
        });
        toast.success(t('messages.createSuccess'));
      }
      onSuccess();
      onOpenChange(false);
      form.reset();
    } catch (error: any) {
      const errorMsg = error.response?.data?.message;
      if (errorMsg === 'Kendi rolünüzü değiştiremezsiniz' || errorMsg === 'Cannot change your own role') {
        toast.error(t('messages.unauthorizedRoleChange'));
      } else if (errorMsg === 'Cannot remove the last admin of the company') {
        toast.error(t('messages.lastAdmin'));
      } else if (errorMsg?.includes('SUPER_ADMIN')) {
        toast.error('SUPER_ADMIN rolü atanamaz veya değiştirilemez');
      } else if (errorMsg?.includes('limitinize')) {
        toast.error(errorMsg);
      } else {
        toast.error(errorMsg || t('messages.error'));
      }
      console.error('Failed to save user', error);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[425px]">
        <DialogHeader>
          <DialogTitle>{user ? t('form.editUser') : t('form.newUser')}</DialogTitle>
          <DialogDescription>
            {user ? t('form.editDescription') : t('form.newDescription')}
          </DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="fullName"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t('form.fullName')}</FormLabel>
                  <FormControl>
                    <Input placeholder="John Doe" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="email"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t('form.email')}</FormLabel>
                  <FormControl>
                    <Input 
                      placeholder="john@example.com" 
                      type="email" 
                      disabled={!!user} // Email cannot be changed
                      {...field} 
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="role"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t('form.role')}</FormLabel>
                  <Select onValueChange={field.onChange} defaultValue={field.value}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder={t('form.selectRole')} />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {roles.map(r => (
                        <SelectItem key={r} value={r}>{r}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />
            
            {!user && (
              <FormField
                control={form.control}
                name="password"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t('form.password')}</FormLabel>
                    <FormControl>
                      <Input type="password" {...field} />
                    </FormControl>
                    <FormMessage />
                    
                    {/* Password requirements checklist */}
                    {password && (
                        <div className="mt-2 space-y-1 bg-muted/30 p-3 rounded-md animate-in fade-in duration-300">
                          <p className="text-xs font-semibold text-muted-foreground mb-1">
                            {tVal('requirements.title')}
                          </p>
                          {passwordRequirements.map((req, index) => (
                            <div
                              key={index}
                              className={`flex items-center text-xs transition-colors duration-200 ${req.regex.test(password)
                                ? 'text-green-600 dark:text-green-400'
                                : 'text-muted-foreground'
                                }`}
                            >
                              <CheckCircle2 className={`h-3 w-3 mr-1 transition-opacity duration-200 ${req.regex.test(password) ? 'opacity-100' : 'opacity-30'
                                }`} />
                              {req.text}
                            </div>
                          ))}
                        </div>
                      )}
                  </FormItem>
                )}
              />
            )}
            
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)} disabled={isLoading}>
                {t('form.cancel')}
              </Button>
              <Button type="submit" disabled={isLoading}>
                {isLoading ? t('form.saving') : t('form.save')}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
