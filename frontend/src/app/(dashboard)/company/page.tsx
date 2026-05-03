'use client';

import { Building2, Mail, Phone, Globe, MapPin, RefreshCw } from "lucide-react"
import { PageHeader } from "@/components/common/page-header"
import { EmptyState } from "@/components/common/empty-state"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from "@/components/ui/badge"
import { useTranslations } from "next-intl"
import { useState, useEffect, useCallback } from "react"
import { companyService, CompanyResponse } from "@/services/company-service"
import { CompanyFormDialog } from "./_components/company-form-dialog"
import { CreateCompanyDialog } from "./_components/create-company-dialog"
import { Pencil, Plus } from "lucide-react"
import authService from "@/services/auth-service"
import { useAuthStore } from "@/stores/auth-store"

export default function CompanyPage() {
  const t = useTranslations('common.pages.company');
  const [company, setCompany] = useState<CompanyResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [addDialogOpen, setAddDialogOpen] = useState(false);
  const setUser = useAuthStore(state => state.setUser);

  const loadCompany = useCallback(async () => {
    try {
      setLoading(true);
      setError(false);
      const data = await companyService.getMyCompany();
      if (data && data.name) {
        setCompany(data);
      } else {
        setError(true);
      }
    } catch {
      setError(true);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadCompany();
  }, [loadCompany]);

  return (
    <div className="space-y-6">
      <PageHeader
        title={t('title')}
        description={t('description')}
        actions={
          <div className="flex gap-2">
            <Button variant="outline" onClick={() => setAddDialogOpen(true)}>
              <Plus className="mr-2 h-4 w-4" />
              {t('addClient') || 'Add Client'}
            </Button>
            {company && (
              <Button onClick={() => setDialogOpen(true)}>
                <Pencil className="mr-2 h-4 w-4" />
                {t('editProfile') || 'Edit Company'}
              </Button>
            )}
          </div>
        }
      />

      {loading ? (
        <div className="flex justify-center items-center py-12">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
        </div>
      ) : error || !company ? (
        <div className="flex flex-col items-center justify-center py-12 space-y-4">
          <EmptyState
            icon={Building2}
            title={t('profileTitle')}
            description={t('profileDescription')}
          />
          <Button variant="outline" onClick={loadCompany}>
            <RefreshCw className="mr-2 h-4 w-4" />
            {t('retry') || 'Retry'}
          </Button>
        </div>
      ) : (
        <div className="grid gap-6 md:grid-cols-2">
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle className="flex items-center gap-2">
                    <Building2 className="h-5 w-5" />
                    {company.name}
                  </CardTitle>
                  <CardDescription>{t('profileTitle')}</CardDescription>
                </div>
                <Badge variant={company.isActive ? 'default' : 'secondary'}>
                  {company.isActive ? (t('active') || 'Active') : (t('inactive') || 'Inactive')}
                </Badge>
              </div>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <p className="text-muted-foreground">{t('taxNumber') || 'Tax Number'}</p>
                  <p className="font-medium">{company.taxNumber || '-'}</p>
                </div>
                <div>
                  <p className="text-muted-foreground">{t('taxOffice') || 'Tax Office'}</p>
                  <p className="font-medium">{company.taxOffice || '-'}</p>
                </div>
                <div>
                  <p className="text-muted-foreground">{t('currency') || 'Currency'}</p>
                  <p className="font-medium">{company.defaultCurrency || '-'}</p>
                </div>
                <div>
                  <p className="text-muted-foreground">{t('invoicePrefix') || 'Invoice Prefix'}</p>
                  <p className="font-medium">{company.invoicePrefix || '-'}</p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>{t('contactTitle') || 'Contact Information'}</CardTitle>
              <CardDescription>{t('contactDescription') || 'Company contact details'}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              {company.email && (
                <div className="flex items-center gap-3">
                  <Mail className="h-4 w-4 text-muted-foreground" />
                  <span className="text-sm">{company.email}</span>
                </div>
              )}
              {company.phone && (
                <div className="flex items-center gap-3">
                  <Phone className="h-4 w-4 text-muted-foreground" />
                  <span className="text-sm">{company.phone}</span>
                </div>
              )}
              {company.website && (
                <div className="flex items-center gap-3">
                  <Globe className="h-4 w-4 text-muted-foreground" />
                  <span className="text-sm">{company.website}</span>
                </div>
              )}
              {(company.address || company.city) && (
                <div className="flex items-center gap-3">
                  <MapPin className="h-4 w-4 text-muted-foreground" />
                  <span className="text-sm">
                    {[company.address, company.district, company.city, company.postalCode]
                      .filter(Boolean)
                      .join(', ')}
                  </span>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      )}

      {company && (
        <CompanyFormDialog
          open={dialogOpen}
          onOpenChange={setDialogOpen}
          company={company}
          onSuccess={loadCompany}
        />
      )}

      <CreateCompanyDialog
        open={addDialogOpen}
        onOpenChange={setAddDialogOpen}
        onSuccess={async () => {
          try {
            const updatedUser = await authService.getCurrentUser();
            setUser(updatedUser);
          } catch (e) {
            console.error("Failed to refresh user data", e);
          }
          // The company switcher in the header should now be updated!
          // Optional: we can set the new company as active, or just let them select it.
        }}
      />
    </div>
  )
}

