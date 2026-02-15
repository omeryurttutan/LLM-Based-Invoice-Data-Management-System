"use client";

import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  CardDescription,
  CardFooter,
} from "@/components/ui/card";
import { PendingActions } from "@/services/dashboard.service";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import Link from "next/link";
import { Badge } from "@/components/ui/badge";
import { ArrowRight, AlertCircle } from "lucide-react";
import { useFormatter, useTranslations } from "next-intl";

interface PendingActionsListProps {
  data?: PendingActions;
  loading: boolean;
}

export function PendingActionsList({ data, loading }: PendingActionsListProps) {
  const t = useTranslations('dashboard');
  const tCommon = useTranslations('common');
  const format = useFormatter();

  if (loading) {
    return (
      <Card className="col-span-1 h-[400px]">
        <CardHeader>
          <CardTitle>{t('pendingActions.title')}</CardTitle>
          <CardDescription>{t('pendingActions.description')}</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {[...Array(5)].map((_, i) => (
            <div key={i} className="flex items-center justify-between">
              <div className="space-y-1">
                <Skeleton className="h-4 w-[150px]" />
                <Skeleton className="h-3 w-[100px]" />
              </div>
              <Skeleton className="h-8 w-[80px]" />
            </div>
          ))}
        </CardContent>
      </Card>
    );
  }

  const invoices = data?.invoices || [];

  return (
    <Card className="col-span-1 h-[400px] flex flex-col">
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle>{t('pendingActions.title')} ({data?.totalPending || 0})</CardTitle>
          <Badge variant="secondary" className="bg-yellow-100 text-yellow-800 hover:bg-yellow-100">
            {data?.totalPending} {tCommon('labels.count')}
          </Badge>
        </div>
        <CardDescription>{t('pendingActions.description')}</CardDescription>
      </CardHeader>
      <CardContent className="flex-1 overflow-auto">
        {invoices.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-center text-muted-foreground p-4">
            <div className="h-12 w-12 rounded-full bg-green-100 flex items-center justify-center mb-4">
              <ArrowRight className="h-6 w-6 text-green-600" />
            </div>
            <p>{t('pendingActions.noPending')}</p>
            <p className="text-sm">{t('pendingActions.allProcessed')}</p>
          </div>
        ) : (
          <div className="space-y-4">
            {invoices.map((invoice) => (
              <div
                key={invoice.id}
                className="flex items-center justify-between border-b pb-4 last:border-0 last:pb-0"
              >
                <div className="space-y-1">
                  <p className="text-sm font-medium leading-none">
                    {invoice.supplierName}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {invoice.invoiceNumber} • {format.number(invoice.totalAmount, { style: 'currency', currency: invoice.currency })}
                  </p>
                  {invoice.daysPending > 7 && (
                    <div className="flex items-center text-xs text-red-500 mt-1">
                      <AlertCircle className="h-3 w-3 mr-1" />
                      <span>{t('pendingActions.daysWaiting', { days: invoice.daysPending })}</span>
                    </div>
                  )}
                </div>
                <Button asChild size="sm" variant="outline">
                  <Link href={`/invoices/${invoice.id}/verify`}>{tCommon('actions.verify')}</Link>
                </Button>
              </div>
            ))}
          </div>
        )}
      </CardContent>
      <CardFooter className="pt-4 border-t">
        <Button asChild variant="ghost" className="w-full">
          <Link href="/invoices?status=PENDING">
            {t('pendingActions.viewAll')} <ArrowRight className="ml-2 h-4 w-4" />
          </Link>
        </Button>
      </CardFooter>
    </Card >
  );
}
