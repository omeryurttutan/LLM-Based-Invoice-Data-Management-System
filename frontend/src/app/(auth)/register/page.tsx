'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Loader2, Mail, Lock, User, AlertCircle, CheckCircle2 } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Alert, AlertDescription } from '@/components/ui/alert';

import { useAuthStore } from '@/stores/auth-store';
import { authService } from '@/services/auth-service';
import { ApiError } from '@/types/auth';
import { useTranslations } from 'next-intl';

function RegisterForm() {
  const t = useTranslations('auth.register');
  const tVal = useTranslations('auth.validation');
  const tCommon = useTranslations('common');
  const router = useRouter();
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const { setAuth } = useAuthStore();

  // Validation schema with translations
  const registerSchema = z.object({
    fullName: z
      .string()
      .min(1, tVal('required'))
      .min(3, tVal('minLength', { min: 3 }))
      .max(100, tVal('maxLength', { max: 100 })), // Assuming maxLength key exists or fallback
    email: z
      .string()
      .min(1, tVal('required'))
      .email(tVal('email')),
    companyName: z
      .string()
      .min(1, tVal('required')),
    taxNumber: z
      .string()
      .regex(/^\d{10}$/, tVal('requirements.taxNumber', { fallback: 'Vergi numarası tam 10 haneli olmalıdır' })),
    password: z
      .string()
      .min(1, tVal('required'))
      .min(8, tVal('minLength', { min: 8 })),
    confirmPassword: z
      .string()
      .min(1, tVal('required')),
  }).refine((data) => data.password === data.confirmPassword, {
    message: tVal('passwordMismatch'),
    path: ['confirmPassword'],
  });

  type RegisterFormData = z.infer<typeof registerSchema>;

  // Password requirements with translations
  // Note: hardcoded for now or use keys if created
  const passwordRequirements = [
    { regex: /.{8,}/, text: tVal('requirements.length', { min: 8 }) },
    { regex: /[A-Z]/, text: tVal('requirements.uppercase') },
    { regex: /[a-z]/, text: tVal('requirements.lowercase') },
    { regex: /[0-9]/, text: tVal('requirements.number') },
    { regex: /[!@#$%^&*(),.?":{}|<>]/, text: tVal('requirements.special') },
  ];

  const {
    register,
    handleSubmit,
    watch,
    setError: setFormError,
    formState: { errors },
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      fullName: '',
      email: '',
      password: '',
      confirmPassword: '',
      companyName: '',
      taxNumber: '',
    },
  });

  const password = watch('password');

  const onSubmit = async (data: RegisterFormData) => {
    setIsLoading(true);
    setError(null);

    try {
      const response = await authService.register({
        fullName: data.fullName,
        email: data.email,
        password: data.password,
        companyName: data.companyName,
        taxNumber: data.taxNumber,
      });

      setAuth(response.user, {
        accessToken: response.accessToken,
        refreshToken: response.refreshToken,
        tokenType: response.tokenType,
        expiresIn: response.expiresIn,
      });

      // Redirect to dashboard
      router.push('/');
      router.refresh();
    } catch (err) {
      const apiError = err as { response?: { data?: ApiError } };

      if (apiError.response?.data?.fieldErrors && apiError.response.data.fieldErrors.length > 0) {
        apiError.response.data.fieldErrors.forEach((fieldError) => {
          setFormError(fieldError.field as keyof RegisterFormData, {
            type: 'server',
            message: fieldError.message,
          });
        });
        setError(tCommon('messages.error.validation', { fallback: 'Lütfen formdaki hataları düzeltin.' }));
      } else if (apiError.response?.data?.message) {
        // Fallback to standard message translation if generic
        if (apiError.response.data.message === 'Validation failed for one or more fields') {
          setError(tCommon('messages.error.validation', { fallback: 'Lütfen tüm alanları doğru formatta doldurun.' }));
        } else {
          setError(apiError.response.data.message);
        }
      } else if (apiError.response?.data?.details) {
        const firstError = Object.values(apiError.response.data.details)[0];
        setError(firstError as string);
      } else {
        setError(tCommon('messages.error.generic'));
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Card className="w-full shadow-lg border-muted">
      <CardHeader className="space-y-1 text-center">
        <CardTitle className="text-2xl font-bold">{t('title')}</CardTitle>
        <CardDescription>
          {t('description')}
        </CardDescription>
      </CardHeader>

      <CardContent>
        {error && (
          <Alert variant="destructive" className="mb-4">
            <AlertCircle className="h-4 w-4" />
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="fullName">{t('fullNameLabel')}</Label>
            <div className="relative">
              <User className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                id="fullName"
                type="text"
                placeholder=""
                className="pl-10"
                disabled={isLoading}
                {...register('fullName')}
              />
            </div>
            {errors.fullName && (
              <p className="text-sm text-destructive">{errors.fullName.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="email">{t('emailLabel')}</Label>
            <div className="relative">
              <Mail className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                id="email"
                type="email"
                placeholder="email@company.com"
                className="pl-10"
                disabled={isLoading}
                {...register('email')}
              />
            </div>
            {errors.email && (
              <p className="text-sm text-destructive">{errors.email.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="companyName">{t('companyName', { fallback: 'Şirket Adı' })}</Label>
            <Input
              id="companyName"
              type="text"
              placeholder="Örnek A.Ş."
              disabled={isLoading}
              {...register('companyName')}
            />
            {errors.companyName && (
              <p className="text-sm text-destructive">{errors.companyName.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="taxNumber">{t('taxNumberLabel', { fallback: 'Vergi No' })}</Label>
            <Input
              id="taxNumber"
              type="text"
              placeholder="1234567890"
              maxLength={10}
              disabled={isLoading}
              {...register('taxNumber')}
            />
            {errors.taxNumber && (
              <p className="text-sm text-destructive">{errors.taxNumber.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="password">{t('passwordLabel')}</Label>
            <div className="relative">
              <Lock className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                id="password"
                type="password"
                placeholder="••••••••"
                className="pl-10"
                disabled={isLoading}
                {...register('password')}
              />
            </div>
            {errors.password && (
              <p className="text-sm text-destructive">{errors.password.message}</p>
            )}

            {/* Password requirements checklist */}
            {password && (
              <div className="mt-2 space-y-1 bg-muted/50 p-3 rounded-md">
                <p className="text-xs font-semibold text-muted-foreground mb-1">{tVal('requirements.title')}</p>
                {passwordRequirements.map((req, index) => (
                  <div
                    key={index}
                    className={`flex items-center text-xs ${req.regex.test(password)
                      ? 'text-green-600 dark:text-green-400'
                      : 'text-muted-foreground'
                      }`}
                  >
                    <CheckCircle2 className={`h-3 w-3 mr-1 ${req.regex.test(password) ? 'opacity-100' : 'opacity-30'
                      }`} />
                    {req.text}
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="confirmPassword">{t('confirmPasswordLabel')}</Label>
            <div className="relative">
              <Lock className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                id="confirmPassword"
                type="password"
                placeholder="••••••••"
                className="pl-10"
                disabled={isLoading}
                {...register('confirmPassword')}
              />
            </div>
            {errors.confirmPassword && (
              <p className="text-sm text-destructive">{errors.confirmPassword.message}</p>
            )}
          </div>

          <Button type="submit" className="w-full" disabled={isLoading}>
            {isLoading ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                {t('submit')}...
              </>
            ) : (
              t('submit')
            )}
          </Button>
        </form>
      </CardContent>

      <CardFooter className="flex flex-col space-y-2 border-t pt-4">
        <div className="text-sm text-muted-foreground text-center">
          {t('haveAccount')}{' '}
          <Link href="/login" className="text-primary hover:underline font-medium">
            {t('loginLink')}
          </Link>
        </div>
      </CardFooter>
    </Card>
  );
}

export default function RegisterPage() {
  return (
    <React.Suspense fallback={<div>Loading...</div>}>
      <RegisterForm />
    </React.Suspense>
  );
}
