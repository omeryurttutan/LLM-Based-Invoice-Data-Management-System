'use client';

import { useAuthStore } from '@/stores/auth-store';
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select';
import { Building2 } from 'lucide-react';
import { useTranslations } from 'next-intl';

export function CompanySwitcher() {
    const { user, activeCompanyId, setActiveCompanyId } = useAuthStore();
    const t = useTranslations('navigation.header');

    if (!user || !user.accessibleCompanies || user.accessibleCompanies.length <= 1) {
        return null;
    }

    const handleCompanyChange = (companyId: string) => {
        setActiveCompanyId(companyId);
        // Refresh the page or relevant components to load data for the new company
        window.location.reload();
    };

    return (
        <div className="flex items-center gap-2">
            <Building2 className="h-4 w-4 text-muted-foreground hidden sm:block" />
            <Select value={activeCompanyId || undefined} onValueChange={handleCompanyChange}>
                <SelectTrigger className="w-[180px] h-9">
                    <SelectValue placeholder={t('selectCompany') || "Şirket Seçin"} />
                </SelectTrigger>
                <SelectContent>
                    {user.accessibleCompanies.map((company) => (
                        <SelectItem key={company.id} value={company.id}>
                            {company.name}
                        </SelectItem>
                    ))}
                </SelectContent>
            </Select>
        </div>
    );
}
