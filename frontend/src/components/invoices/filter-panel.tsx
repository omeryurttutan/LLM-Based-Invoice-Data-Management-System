'use client';

import { useState, useEffect } from 'react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { useInvoiceFilters } from '@/hooks/use-invoice-filters';
import { useFilterOptions } from '@/hooks/use-filter-options';
import { InvoiceListParams } from '@/types/invoice';
import { ChevronDown, ChevronUp, Filter } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible';
import {
    StatusFilter, DateRangeFilter, AmountRangeFilter, SupplierFilter, CategoryFilter,
    CurrencyFilter, SourceTypeFilter
} from './filters';
import { useTranslations } from 'next-intl';

export function FilterPanel() {
    const t = useTranslations('invoices.filters');
    const tCommon = useTranslations('common');
    const { filters, setFilters, clearFilters, activeFilterCount } = useInvoiceFilters();
    const { data: options, isLoading: isLoadingOptions } = useFilterOptions();
    const [isOpen, setIsOpen] = useState(false);

    // ... (rest of the component state)

    // Local state for pending changes
    const [localFilters, setLocalFilters] = useState<InvoiceListParams>(filters);

    // Sync active filters to local state when panel opens or external changes happen
    useEffect(() => {
        setLocalFilters(filters);
    }, [filters, isOpen]);

    const handleApply = () => {
        setFilters(localFilters);
        // setIsOpen(false); 
    };

    const handleClear = () => {
        clearFilters();
    };

    return (
        <div className="w-full space-y-2">
            <div className="flex items-center justify-between">
                {/* ... Collapsible Trigger ... */}
                <Collapsible open={isOpen} onOpenChange={setIsOpen} className="w-full">
                    <CollapsibleTrigger asChild>
                        <Button variant="outline" className="flex gap-2 w-full justify-between sm:w-auto sm:justify-start">
                            <div className="flex items-center gap-2">
                                <Filter className="h-4 w-4" />
                                <span>{t('title')}</span>
                                {activeFilterCount > 0 && <Badge variant="secondary" className="ml-1 rounded-full px-2">{activeFilterCount}</Badge>}
                            </div>
                            {isOpen ? <ChevronUp className="h-4 w-4 ml-2" /> : <ChevronDown className="h-4 w-4 ml-2" />}
                        </Button>
                    </CollapsibleTrigger>
                    <CollapsibleContent className="mt-4">
                        <Card>
                            <CardHeader className="pb-3 border-b mb-3 bg-muted/20">
                                <CardTitle className="text-base font-medium">{t('detailedOptions')}</CardTitle>
                            </CardHeader>
                            <CardContent className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
                                {/* Row 1 */}
                                <DateRangeFilter
                                    value={{ from: localFilters.dateFrom, to: localFilters.dateTo }}
                                    onChange={(val) => setLocalFilters({ ...localFilters, dateFrom: val.from, dateTo: val.to })}
                                />
                                <StatusFilter
                                    value={localFilters.status}
                                    onChange={(val) => setLocalFilters({ ...localFilters, status: val })}
                                    options={options?.statuses}
                                    isLoading={isLoadingOptions}
                                />
                                <CategoryFilter
                                    value={localFilters.categoryId}
                                    onChange={(val) => setLocalFilters({ ...localFilters, categoryId: val })}
                                    options={options?.categories}
                                    isLoading={isLoadingOptions}
                                />

                                {/* Row 2 */}
                                <SupplierFilter
                                    value={localFilters.supplierName}
                                    onChange={(val) => setLocalFilters({ ...localFilters, supplierName: val })}
                                />
                                <AmountRangeFilter
                                    value={{ min: localFilters.amountMin, max: localFilters.amountMax }}
                                    onChange={(val) => setLocalFilters({ ...localFilters, amountMin: val.min, amountMax: val.max })}
                                />
                                <CurrencyFilter
                                    value={localFilters.currency}
                                    onChange={(val) => setLocalFilters({ ...localFilters, currency: val })}
                                    options={options?.currencies?.length ? options.currencies : ['TRY', 'USD', 'EUR', 'GBP']}
                                    isLoading={isLoadingOptions}
                                />

                                {/* Row 3 */}
                                <SourceTypeFilter
                                    value={localFilters.sourceType}
                                    onChange={(val) => setLocalFilters({ ...localFilters, sourceType: val })}
                                    options={options?.sourceTypes?.length ? options.sourceTypes : ['LLM', 'E_INVOICE', 'MANUAL']}
                                    isLoading={isLoadingOptions}
                                />
                            </CardContent>
                            <CardFooter className="flex justify-end gap-2 border-t p-4 bg-muted/20">
                                <Button variant="ghost" onClick={handleClear} className="text-muted-foreground hover:text-destructive">
                                    {t('clear')}
                                </Button>
                                <Button onClick={handleApply} className="bg-primary text-primary-foreground hover:bg-primary/90">
                                    {t('apply')}
                                </Button>
                            </CardFooter>
                        </Card>
                    </CollapsibleContent>
                </Collapsible>
            </div>
        </div>
    );
}
