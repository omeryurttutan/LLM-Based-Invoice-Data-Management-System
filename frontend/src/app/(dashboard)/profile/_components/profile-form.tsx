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
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { UserProfileResponse } from '@/types/profile';
import { profileService } from '@/services/profile-service';

interface ProfileFormProps {
  profile: UserProfileResponse;
  onUpdate: (updated: UserProfileResponse) => void;
}

export function ProfileForm({ profile, onUpdate }: ProfileFormProps) {
  const t = useTranslations('common.pages.profile');
  const [isLoading, setIsLoading] = useState(false);

  const formSchema = z.object({
    fullName: z.string().min(2, { message: t('form.nameRequired') }),
    phone: z.string().optional(),
  });

  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      fullName: profile.fullName || '',
      phone: profile.phone || '',
    },
  });

  const onSubmit = async (values: z.infer<typeof formSchema>) => {
    try {
      setIsLoading(true);
      const updated = await profileService.updateProfile({
        fullName: values.fullName,
        phone: values.phone,
      });
      onUpdate(updated);
      toast.success(t('messages.updateSuccess'));
    } catch (error) {
      toast.error(t('messages.error'));
      console.error(error);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t('profileTitle')}</CardTitle>
        <CardDescription>{t('profileDescription')}</CardDescription>
      </CardHeader>
      <CardContent>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4 max-w-md">
            <div className="space-y-2">
              <Label>{t('form.email')}</Label>
              <Input value={profile.email} disabled />
              <p className="text-[0.8rem] text-muted-foreground">{t('form.emailHelp')}</p>
            </div>

            <div className="space-y-2">
              <Label>{t('form.role')}</Label>
              <Input value={profile.role} disabled />
            </div>

            <div className="space-y-2">
              <Label>{t('form.company')}</Label>
              <Input value={profile.companyName} disabled />
            </div>

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
              name="phone"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t('form.phone') || 'Phone'}</FormLabel>
                  <FormControl>
                    <Input placeholder="+905551234567" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <Button type="submit" disabled={isLoading || !form.formState.isDirty}>
              {isLoading ? t('form.saving') : t('save')}
            </Button>
          </form>
        </Form>
      </CardContent>
    </Card>
  );
}
