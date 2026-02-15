import React from 'react';
import { formatDistanceToNow } from 'date-fns';
import { tr } from 'date-fns/locale';
import {
    Pencil,
    Sparkles,
    RefreshCw,
    CheckCircle,
    ArrowRightLeft,
    Undo,
    Layers,
    ArrowRight,
    GitCompare,
    History
} from 'lucide-react';

import { cn } from '@/lib/utils';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip';

import { InvoiceVersionSummary, ChangeSource } from '@/types/version-history';

// Icon mapping for change sources
const sourceIcons: Record<ChangeSource, React.ElementType> = {
    [ChangeSource.MANUAL_EDIT]: Pencil,
    [ChangeSource.LLM_EXTRACTION]: Sparkles,
    [ChangeSource.LLM_RE_EXTRACTION]: RefreshCw,
    [ChangeSource.VERIFICATION]: CheckCircle,
    [ChangeSource.STATUS_CHANGE]: ArrowRightLeft,
    [ChangeSource.REVERT]: Undo,
    [ChangeSource.BULK_UPDATE]: Layers,
};

// Color mapping for change sources (using Tailwind classes)
const sourceColors: Record<ChangeSource, string> = {
    [ChangeSource.MANUAL_EDIT]: 'text-blue-500 bg-blue-50 border-blue-200',
    [ChangeSource.LLM_EXTRACTION]: 'text-purple-500 bg-purple-50 border-purple-200',
    [ChangeSource.LLM_RE_EXTRACTION]: 'text-purple-500 bg-purple-50 border-purple-200',
    [ChangeSource.VERIFICATION]: 'text-green-500 bg-green-50 border-green-200',
    [ChangeSource.STATUS_CHANGE]: 'text-yellow-500 bg-yellow-50 border-yellow-200',
    [ChangeSource.REVERT]: 'text-orange-500 bg-orange-50 border-orange-200',
    [ChangeSource.BULK_UPDATE]: 'text-gray-500 bg-gray-50 border-gray-200',
};

// Turkish labels for change sources
const sourceLabels: Record<ChangeSource, string> = {
    [ChangeSource.MANUAL_EDIT]: 'Manuel Düzenleme',
    [ChangeSource.LLM_EXTRACTION]: 'LLM Çıkarımı',
    [ChangeSource.LLM_RE_EXTRACTION]: 'Yeniden Çıkarım',
    [ChangeSource.VERIFICATION]: 'Doğrulama',
    [ChangeSource.STATUS_CHANGE]: 'Durum Değişikliği',
    [ChangeSource.REVERT]: 'Geri Alma',
    [ChangeSource.BULK_UPDATE]: 'Toplu Güncelleme',
};

interface VersionTimelineProps {
    versions: InvoiceVersionSummary[];
    currentVersion?: number;
    onCompare: (version: InvoiceVersionSummary) => void;
    onRevertRequest: (version: InvoiceVersionSummary) => void;
    canRevert: boolean;
}

export function VersionTimeline({
    versions,
    currentVersion,
    onCompare,
    onRevertRequest,
    canRevert
}: VersionTimelineProps) {

    if (!versions || versions.length === 0) {
        return (
            <div className="flex flex-col items-center justify-center p-8 text-center text-muted-foreground">
                <History className="h-12 w-12 mb-4 opacity-20" />
                <p>Bu fatura için henüz versiyon geçmişi bulunmuyor.</p>
            </div>
        );
    }

    // Find latest version if not provided
    const latestVersionNum = currentVersion || Math.max(...versions.map(v => v.versionNumber));

    return (
        <Card className="h-full flex flex-col">
            <CardHeader>
                <CardTitle>Versiyon Geçmişi</CardTitle>
                <CardDescription>
                    Toplam {versions.length} versiyon kaydı bulundu
                </CardDescription>
            </CardHeader>
            <CardContent className="flex-1 p-0">
                <ScrollArea className="h-[600px] w-full p-6">
                    <div className="relative border-l border-muted ml-4 space-y-8">
                        {versions.map((version, index) => {
                            const Icon = sourceIcons[version.changeSource] || Pencil;
                            const isLatest = version.versionNumber === latestVersionNum;
                            const colorClass = sourceColors[version.changeSource] || 'text-gray-500 bg-gray-50 border-gray-200';

                            return (
                                <div key={version.id} className="relative pl-8">
                                    {/* Timeline dot/badge */}
                                    <div className={cn(
                                        "absolute -left-[2.75rem] flex h-6 w-6 items-center justify-center rounded-full border bg-background text-xs font-bold",
                                        isLatest ? "border-primary text-primary" : "border-muted text-muted-foreground"
                                    )}>
                                        v{version.versionNumber}
                                    </div>

                                    {/* Icon Indicator */}
                                    <div className={cn(
                                        "absolute -left-3 flex h-6 w-6 items-center justify-center rounded-full border bg-background",
                                        colorClass
                                    )}>
                                        <Icon className="h-3 w-3" />
                                    </div>

                                    <div className="flex flex-col space-y-2">
                                        <div className="flex items-center justify-between">
                                            <div className="flex items-center gap-2">
                                                <Badge variant="outline" className={cn("text-xs font-normal", colorClass)}>
                                                    {sourceLabels[version.changeSource]}
                                                </Badge>
                                                {isLatest && (
                                                    <Badge variant="default" className="text-xs bg-green-600 hover:bg-green-700">
                                                        Güncel
                                                    </Badge>
                                                )}
                                                <span className="text-xs text-muted-foreground">
                                                    {formatDistanceToNow(new Date(version.createdAt), { addSuffix: true, locale: tr })}
                                                </span>
                                            </div>
                                            <div className="flex gap-1">
                                                {!isLatest && (
                                                    <TooltipProvider>
                                                        <Tooltip>
                                                            <TooltipTrigger asChild>
                                                                <Button
                                                                    variant="ghost"
                                                                    size="icon"
                                                                    className="h-8 w-8"
                                                                    onClick={() => onCompare(version)}
                                                                >
                                                                    <GitCompare className="h-4 w-4" />
                                                                </Button>
                                                            </TooltipTrigger>
                                                            <TooltipContent>Mevcut ile karşılaştır</TooltipContent>
                                                        </Tooltip>
                                                    </TooltipProvider>
                                                )}

                                                {!isLatest && canRevert && (
                                                    <TooltipProvider>
                                                        <Tooltip>
                                                            <TooltipTrigger asChild>
                                                                <Button
                                                                    variant="ghost"
                                                                    size="icon"
                                                                    className="h-8 w-8 text-destructive hover:text-destructive hover:bg-destructive/10"
                                                                    onClick={() => onRevertRequest(version)}
                                                                >
                                                                    <Undo className="h-4 w-4" />
                                                                </Button>
                                                            </TooltipTrigger>
                                                            <TooltipContent>Bu versiyona geri dön</TooltipContent>
                                                        </Tooltip>
                                                    </TooltipProvider>
                                                )}
                                            </div>
                                        </div>

                                        <div className="text-sm font-medium">
                                            {version.changeSummary || 'Değişiklik özeti yok'}
                                        </div>

                                        <div className="flex flex-wrap gap-1">
                                            {version.changedFields && version.changedFields.length > 0 ? (
                                                version.changedFields.map((field, i) => (
                                                    <Badge key={i} variant="secondary" className="text-[10px] px-1 py-0 h-5">
                                                        {field}
                                                    </Badge>
                                                ))
                                            ) : (
                                                <span className="text-xs text-muted-foreground italic">Alan değişikliği yok</span>
                                            )}
                                        </div>

                                        <div className="flex items-center gap-2 text-xs text-muted-foreground mt-1">
                                            <span className="font-medium text-foreground">{version.changedBy}</span>
                                            <span>tarafından değiştirildi</span>
                                        </div>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                </ScrollArea>
            </CardContent>
        </Card>
    );
}
