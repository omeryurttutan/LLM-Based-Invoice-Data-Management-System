import React, { useState } from 'react';
import { format } from 'date-fns';
import { tr } from 'date-fns/locale';
import { Loader2 } from 'lucide-react';

import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { Button } from '@/components/ui/button';
import { InvoiceVersionSummary } from '@/types/version-history';
import { versionService } from '@/services/version-service';
import { toast } from 'sonner';

interface RevertDialogProps {
    invoiceId: string;
    version: InvoiceVersionSummary | null;
    open: boolean;
    onOpenChange: (open: boolean) => void;
    onSuccess: () => void;
}

export function RevertDialog({
    invoiceId,
    version,
    open,
    onOpenChange,
    onSuccess
}: RevertDialogProps) {
    const [isReverting, setIsReverting] = useState(false);

    if (!version) return null;

    const handleRevert = async () => {
        try {
            setIsReverting(true);
            await versionService.revertToVersion(invoiceId, version.versionNumber);

            toast.success(`Fatura v${version.versionNumber} versiyonuna başarıyla geri döndürüldü.`);
            onOpenChange(false);
            onSuccess();
        } catch (error) {
            console.error('Revert failed:', error);
            toast.error('Geri alma işlemi başarısız oldu.');
        } finally {
            setIsReverting(false);
        }
    };

    return (
        <AlertDialog open={open} onOpenChange={onOpenChange}>
            <AlertDialogContent>
                <AlertDialogHeader>
                    <AlertDialogTitle>Versiyona Geri Dön</AlertDialogTitle>
                    <AlertDialogDescription className="space-y-2">
                        <p>
                            Fatura verilerini <strong>v{version.versionNumber}</strong> versiyonuna geri döndürmek üzeresiniz.
                        </p>
                        <div className="bg-muted p-3 rounded-md text-sm border">
                            <div className="grid grid-cols-3 gap-1">
                                <span className="text-muted-foreground">Tarih:</span>
                                <span className="col-span-2 font-medium">
                                    {format(new Date(version.createdAt), 'd MMMM yyyy HH:mm', { locale: tr })}
                                </span>

                                <span className="text-muted-foreground">İşlem:</span>
                                <span className="col-span-2 font-medium">{version.changeSummary}</span>

                                <span className="text-muted-foreground">Kullanıcı:</span>
                                <span className="col-span-2 font-medium">{version.changedBy}</span>
                            </div>
                        </div>
                        <p className="font-medium text-amber-600 dark:text-amber-500 mt-2">
                            Bu işlem mevcut durumu yeni bir versiyon olarak kaydeder ve veri kaybına neden olmaz.
                        </p>
                    </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                    <AlertDialogCancel disabled={isReverting}>İptal</AlertDialogCancel>
                    <Button
                        variant="default"
                        onClick={(e) => {
                            e.preventDefault();
                            handleRevert();
                        }}
                        disabled={isReverting}
                    >
                        {isReverting ? (
                            <>
                                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                                Geri Alınıyor...
                            </>
                        ) : (
                            'Geri Al'
                        )}
                    </Button>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
}
