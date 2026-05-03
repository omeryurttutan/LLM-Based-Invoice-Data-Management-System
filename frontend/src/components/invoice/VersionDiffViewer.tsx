import React from 'react';
import {
    ArrowRight,
    ArrowLeft,
    ArrowRightLeft,
    MinusCircle,
    PlusCircle,
    AlertCircle
} from 'lucide-react';
import { format } from 'date-fns';
import { tr } from 'date-fns/locale';

import { cn, formatCurrency } from '@/lib/utils';
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Separator } from '@/components/ui/separator';

import {
    VersionDiff,
    FieldChange,
    ItemChange,
    ChangeType
} from '@/types/version-history';

interface VersionDiffViewerProps {
    diff: VersionDiff;
    previousVersionNum: number;
    currentVersionNum: number;
}

export function VersionDiffViewer({
    diff,
    previousVersionNum,
    currentVersionNum
}: VersionDiffViewerProps) {

    if (!diff || (!diff.changes.length && !diff.itemChanges.length)) {
        return (
            <div className="flex flex-col items-center justify-center p-8 text-center text-muted-foreground min-h-[300px]">
                <AlertCircle className="h-10 w-10 mb-4 opacity-20" />
                <p>Seçilen versiyonlar arasında herhangi bir fark bulunamadı.</p>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between bg-muted/30 p-4 rounded-lg border">
                <div className="flex items-center gap-2">
                    <Badge variant="outline" className="bg-background">v{previousVersionNum}</Badge>
                    <span className="text-sm text-muted-foreground">Eski Versiyon</span>
                </div>
                <ArrowRight className="h-4 w-4 text-muted-foreground" />
                <div className="flex items-center gap-2">
                    <span className="text-sm text-muted-foreground">Yeni Versiyon</span>
                    <Badge variant="default">v{currentVersionNum}</Badge>
                </div>
            </div>

            {diff.changes.length > 0 && (
                <Card>
                    <CardHeader className="pb-3">
                        <CardTitle className="text-base">Alan Değişiklikleri</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <Table>
                            <TableHeader>
                                <TableRow>
                                    <TableHead className="w-1/3">Alan</TableHead>
                                    <TableHead className="w-1/3">Eski Değer (v{previousVersionNum})</TableHead>
                                    <TableHead className="w-1/3">Yeni Değer (v{currentVersionNum})</TableHead>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {diff.changes.map((change, index) => (
                                    <FieldChangeRow key={index} change={change} />
                                ))}
                            </TableBody>
                        </Table>
                    </CardContent>
                </Card>
            )}

            {diff.itemChanges.length > 0 && (
                <Card>
                    <CardHeader className="pb-3">
                        <CardTitle className="text-base">Kalem Değişiklikleri</CardTitle>
                    </CardHeader>
                    <CardContent>
                        {/* Simplified item diff view */}
                        <div className="space-y-4">
                            {diff.itemChanges.map((item, index) => (
                                <ItemChangeRow key={index} item={item} />
                            ))}
                        </div>
                    </CardContent>
                </Card>
            )}
        </div>
    );
}

function FieldChangeRow({ change }: { change: FieldChange }) {
    const { fieldLabel, oldValue, newValue, changeType } = change;

    // Helper to format value based on content
    const formatValue = (val: any) => {
        if (val === null || val === undefined) return <span className="text-muted-foreground italic">—</span>;
        if (typeof val === 'boolean') return val ? 'Evet' : 'Hayır';

        // Try to detect currency or date
        // Note: Backend might send raw numbers for amounts
        if (typeof val === 'number') {
            // Heuristic: if field name contains 'Amount', 'Price', 'Total', format as currency
            // Ideally we should use field types, but name check is okay for display
            if (/tutar|fiyat|toplam|kdv|amount|price|total/i.test(change.fieldName)) {
                // Default to TRY if we don't know the currency context here
                // In a real app we'd pass currency down
                return formatCurrency(val, 'TRY'); // We might need to pass currency from parent
            }
            // Date check (timestamps usually large numbers) unlikely here as strings preferred
        }

        // String date check (YYYY-MM-DD)
        if (typeof val === 'string' && /^\d{4}-\d{2}-\d{2}/.test(val)) {
            try {
                return format(new Date(val), 'd MMMM yyyy', { locale: tr });
            } catch (e) { console.log(e); return val; }
        }

        return String(val);
    };

    return (
        <TableRow>
            <TableCell className="font-medium">{fieldLabel}</TableCell>
            <TableCell className={cn(
                "bg-red-50/50 dark:bg-red-950/20 text-red-700 dark:text-red-400",
                changeType === ChangeType.ADDED && "opacity-50"
            )}>
                {changeType === ChangeType.ADDED ? <span className="text-muted-foreground italic">Değer yoktu</span> : formatValue(oldValue)}
            </TableCell>
            <TableCell className={cn(
                "bg-green-50/50 dark:bg-green-950/20 text-green-700 dark:text-green-400",
                changeType === ChangeType.REMOVED && "opacity-50"
            )}>
                {changeType === ChangeType.REMOVED ? <span className="text-muted-foreground italic">Silindi</span> : formatValue(newValue)}
            </TableCell>
        </TableRow>
    );
}

function ItemChangeRow({ item }: { item: ItemChange }) {
    const { changeType, description, totalAmount, changes } = item;

    if (changeType === ChangeType.ADDED) {
        return (
            <div className="flex items-start gap-3 p-3 rounded-md bg-green-50 dark:bg-green-950/20 border border-green-100 dark:border-green-900">
                <PlusCircle className="h-5 w-5 text-green-600 mt-0.5" />
                <div>
                    <p className="text-sm font-medium text-green-900 dark:text-green-300">Yeni Kalem Eklendi</p>
                    <p className="text-sm text-green-700 dark:text-green-400">
                        {description || 'Açıklama yok'} - {totalAmount ? formatCurrency(totalAmount, 'TRY') : ''}
                    </p>
                </div>
            </div>
        );
    }

    if (changeType === ChangeType.REMOVED) {
        return (
            <div className="flex items-start gap-3 p-3 rounded-md bg-red-50 dark:bg-red-950/20 border border-red-100 dark:border-red-900">
                <MinusCircle className="h-5 w-5 text-red-600 mt-0.5" />
                <div>
                    <p className="text-sm font-medium text-red-900 dark:text-red-300">Kalem Silindi</p>
                    <p className="text-sm text-red-700 dark:text-red-400 line-through">
                        {description || 'Açıklama yok'} - {totalAmount ? formatCurrency(totalAmount, 'TRY') : ''}
                    </p>
                </div>
            </div>
        );
    }

    return (
        <div className="flex items-start gap-3 p-3 rounded-md bg-blue-50 dark:bg-blue-950/20 border border-blue-100 dark:border-blue-900">
            <ArrowRightLeft className="h-5 w-5 text-blue-600 mt-0.5" />
            <div className="w-full">
                <p className="text-sm font-medium text-blue-900 dark:text-blue-300">Kalem Düzenlendi</p>
                <p className="text-xs text-blue-700 dark:text-blue-400 mb-2">
                    {description}
                </p>
                {changes && changes.length > 0 && (
                    <div className="grid grid-cols-1 gap-1 pl-2 border-l-2 border-blue-200">
                        {changes.map((c, i) => (
                            <div key={i} className="text-xs grid grid-cols-3 gap-2">
                                <span className="font-medium text-blue-800 dark:text-blue-300">{c.fieldLabel}:</span>
                                <span className="text-red-600 line-through dark:text-red-400">{String(c.oldValue)}</span>
                                <span className="text-green-600 dark:text-green-400">{String(c.newValue)}</span>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
}
