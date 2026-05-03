import { useState } from 'react';
import { useTranslations } from 'next-intl';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { profileService } from '@/services/profile-service';
import { CheckCircle2 } from 'lucide-react';

export function PasswordForm() {
  const t = useTranslations('common.pages.profile');
  const tVal = useTranslations('auth.validation');
  const tCommon = useTranslations('common');
  const [isLoading, setIsLoading] = useState(false);

  const formSchema = z.object({
    currentPassword: z.string().min(1, { message: t('messages.passwordError') }),
    newPassword: z.string()
      .min(8, { message: tVal('requirements.length', { min: 8 }) })
      .regex(/^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).*$/, { 
        message: tVal('requirements.title') 
      }),
    confirmPassword: z.string().min(1, { message: t('form.passwordMin') }),
  }).refine((data) => data.newPassword === data.confirmPassword, {
    message: t('form.passwordMatch'),
    path: ["confirmPassword"],
  });

  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      currentPassword: '',
      newPassword: '',
      confirmPassword: '',
    },
  });

  const newPassword = form.watch('newPassword');

  const passwordRequirements = [
    { regex: /.{8,}/, text: tVal('requirements.length', { min: 8 }) },
    { regex: /[A-Z]/, text: tVal('requirements.uppercase') },
    { regex: /[a-z]/, text: tVal('requirements.lowercase') },
    { regex: /[0-9]/, text: tVal('requirements.number') },
    { regex: /[!@#$%^&*(),.?":{}|<>]/, text: tVal('requirements.special') },
  ];

  const onSubmit = async (values: z.infer<typeof formSchema>) => {
    try {
      setIsLoading(true);
      await profileService.changePassword({
        currentPassword: values.currentPassword,
        newPassword: values.newPassword,
      });
      toast.success(t('messages.passwordSuccess'));
      form.reset({
        currentPassword: '',
        newPassword: '',
        confirmPassword: '',
      });
    } catch (error: any) {
      const message = error.response?.data?.message || t('messages.passwordError');
      toast.error(message);
      console.error(error);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t('passwordTitle')}</CardTitle>
        <CardDescription>{t('passwordDescription')}</CardDescription>
      </CardHeader>
      <CardContent>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4 max-w-md">
            <FormField
              control={form.control}
              name="currentPassword"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t('form.currentPassword')}</FormLabel>
                  <FormControl>
                    <Input type="password" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            
            <FormField
              control={form.control}
              name="newPassword"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t('form.newPassword')}</FormLabel>
                  <FormControl>
                    <Input type="password" {...field} />
                  </FormControl>
                  <FormMessage />
                  
                  {/* Password requirements checklist */}
                  {newPassword && (
                    <div className="mt-2 space-y-1 bg-muted/30 p-3 rounded-md animate-in fade-in duration-300">
                      <p className="text-xs font-semibold text-muted-foreground mb-1">
                        {tVal('requirements.title')}
                      </p>
                      {passwordRequirements.map((req, index) => (
                        <div
                          key={index}
                          className={`flex items-center text-xs transition-colors duration-200 ${req.regex.test(newPassword)
                            ? 'text-green-600 dark:text-green-400'
                            : 'text-muted-foreground'
                            }`}
                        >
                          <CheckCircle2 className={`h-3 w-3 mr-1 transition-opacity duration-200 ${req.regex.test(newPassword) ? 'opacity-100' : 'opacity-30'
                            }`} />
                          {req.text}
                        </div>
                      ))}
                    </div>
                  )}
                </FormItem>
              )}
            />
            
            <FormField
              control={form.control}
              name="confirmPassword"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t('form.confirmPassword')}</FormLabel>
                  <FormControl>
                    <Input type="password" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <Button type="submit" disabled={isLoading || !form.formState.isDirty}>
              {isLoading ? t('form.saving') : t('form.changePassword')}
            </Button>
          </form>
        </Form>
      </CardContent>
    </Card>
  );
}
