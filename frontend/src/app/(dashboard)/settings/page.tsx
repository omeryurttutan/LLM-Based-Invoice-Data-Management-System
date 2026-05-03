'use client';

import { PageHeader } from "@/components/common/page-header"
import { useTranslations, useLocale } from "next-intl"
import { useState, useEffect } from "react"
import { settingsService } from "@/services/settings-service"
import { toast } from "sonner"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Label } from "@/components/ui/label"
import { Switch } from "@/components/ui/switch"
import { Button } from "@/components/ui/button"
import { useTheme } from "next-themes"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"

export default function SettingsPage() {
  const t = useTranslations('common.pages.settings');
  const tNav = useTranslations('navigation.header.theme');
  const locale = useLocale();
  const { theme, setTheme } = useTheme();
  
  const [preferences, setPreferences] = useState<Record<string, boolean>>({
    EMAIL_NOTIFICATIONS: false,
    SYSTEM_NOTIFICATIONS: true,
  });
  const [notifLoading, setNotifLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    const loadPreferences = async () => {
      try {
        setNotifLoading(true);
        const data = await settingsService.getNotificationPreferences();
        setPreferences(data);
      } catch (error) {
        console.error(error);
      } finally {
        setNotifLoading(false);
      }
    };
    loadPreferences();
  }, []);

  const handleToggle = (key: string, checked: boolean) => {
    setPreferences(prev => ({ ...prev, [key]: checked }));
  };

  const handleSave = async () => {
    try {
      setSaving(true);
      await settingsService.updateNotificationPreferences(preferences);
      toast.success(t('messages.saveSuccess'));
    } catch (error) {
      toast.error(t('messages.saveError'));
    } finally {
      setSaving(false);
    }
  };

  const handleLanguageChange = (newLocale: string) => {
    document.cookie = `NEXT_LOCALE=${newLocale}; path=/; max-age=31536000`;
    window.location.reload();
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title={t('title')}
        description={t('description')}
      />
      
      <div className="grid gap-6 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>{t('generalTitle')}</CardTitle>
            <CardDescription>{t('generalDescription')}</CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="space-y-2">
              <Label>{t('theme')}</Label>
              <Select value={theme} onValueChange={setTheme}>
                <SelectTrigger>
                  <SelectValue placeholder={t('theme')} />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="light">{tNav('light')}</SelectItem>
                  <SelectItem value="dark">{tNav('dark')}</SelectItem>
                  <SelectItem value="system">{tNav('system')}</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label>{t('language')}</Label>
              <Select value={locale} onValueChange={handleLanguageChange}>
                <SelectTrigger>
                  <SelectValue placeholder={t('language')} />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="tr">Türkçe</SelectItem>
                  <SelectItem value="en">English</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>{t('notificationsTitle')}</CardTitle>
            <CardDescription>{t('notificationsDescription')}</CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            {notifLoading ? (
              <div className="flex justify-center items-center py-4">
                <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-primary"></div>
              </div>
            ) : (
              <>
                <div className="flex items-center justify-between space-x-2">
                  <Label htmlFor="email_notifications" className="flex flex-col space-y-1">
                    <span>{t('emailNotifications')}</span>
                    <span className="font-normal text-sm text-muted-foreground">{t('emailNotificationsHint')}</span>
                  </Label>
                  <Switch 
                    id="email_notifications" 
                    checked={preferences['EMAIL_NOTIFICATIONS'] || false} 
                    onCheckedChange={(checked: boolean) => handleToggle('EMAIL_NOTIFICATIONS', checked)} 
                  />
                </div>
                <div className="flex items-center justify-between space-x-2">
                  <Label htmlFor="system_notifications" className="flex flex-col space-y-1">
                    <span>{t('systemNotifications')}</span>
                    <span className="font-normal text-sm text-muted-foreground">{t('systemNotificationsHint')}</span>
                  </Label>
                  <Switch 
                    id="system_notifications" 
                    checked={preferences['SYSTEM_NOTIFICATIONS'] || false} 
                    onCheckedChange={(checked: boolean) => handleToggle('SYSTEM_NOTIFICATIONS', checked)} 
                  />
                </div>
                <Button onClick={handleSave} disabled={saving}>
                  {saving ? t('saving') : t('save')}
                </Button>
              </>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}

