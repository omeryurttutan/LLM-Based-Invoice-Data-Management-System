'use client';

import { PageHeader } from "@/components/common/page-header"
import { useTranslations } from "next-intl"
import { useState, useEffect, useCallback } from "react"
import { UserProfileResponse } from "@/types/profile"
import { profileService } from "@/services/profile-service"
import { toast } from "sonner"
import { ProfileForm } from "./_components/profile-form"
import { PasswordForm } from "./_components/password-form"
import { Button } from "@/components/ui/button"
import { RefreshCw } from "lucide-react"

export default function ProfilePage() {
  const t = useTranslations('common.pages.profile');
  const [profile, setProfile] = useState<UserProfileResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  const loadProfile = useCallback(async () => {
    try {
      setLoading(true);
      setError(false);
      const data = await profileService.getProfile();
      if (data && data.email) {
        setProfile(data);
      } else {
        setError(true);
      }
    } catch (err) {
      toast.error(t('messages.error'));
      setError(true);
      console.error(err);
    } finally {
      setLoading(false);
    }
  }, [t]);

  useEffect(() => {
    loadProfile();
  }, [loadProfile]);

  return (
    <div className="space-y-6">
      <PageHeader
        title={t('title')}
        description={t('description')}
      />
      
      {loading ? (
        <div className="flex justify-center items-center py-12">
           <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
        </div>
      ) : error || !profile ? (
        <div className="flex flex-col items-center justify-center py-12 space-y-4">
          <p className="text-muted-foreground">{t('messages.error')}</p>
          <Button variant="outline" onClick={loadProfile}>
            <RefreshCw className="mr-2 h-4 w-4" />
            {t('retry') || 'Retry'}
          </Button>
        </div>
      ) : (
        <div className="grid gap-6 md:grid-cols-2">
          <ProfileForm profile={profile} onUpdate={setProfile} />
          <PasswordForm />
        </div>
      )}
    </div>
  )
}

