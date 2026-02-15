/* eslint-disable @typescript-eslint/no-explicit-any */
"use client";

import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Activity, Server, Database, HardDrive, Cpu, AlertTriangle, CheckCircle2, RotateCcw } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import { useSystemStatus } from "@/hooks/use-dashboard";
import { Button } from "@/components/ui/button";
import { useFormatter, useTranslations } from "next-intl";

export function SystemHealthPanel() {
    const { data, isLoading, error, refetch } = useSystemStatus(true);
    const t = useTranslations('dashboard.system');
    const tCommon = useTranslations('common');
    const format = useFormatter();

    if (isLoading) {
        return (
            <Card className="col-span-full border-t-4 border-t-blue-500">
                <CardHeader>
                    <CardTitle>{t('title')}</CardTitle>
                    <CardDescription>{t('description')}</CardDescription>
                </CardHeader>
                <CardContent className="h-[200px] flex items-center justify-center">
                    <Skeleton className="h-[150px] w-full" />
                </CardContent>
            </Card>
        );
    }

    if (error) {
        return (
            <Card className="col-span-full border-t-4 border-t-red-500">
                <CardHeader>
                    <CardTitle>{t('title')}</CardTitle>
                </CardHeader>
                <CardContent className="h-[200px] flex flex-col items-center justify-center text-center gap-4">
                    <div className="flex flex-col items-center text-muted-foreground">
                        <AlertTriangle className="h-8 w-8 text-red-500 mb-2" />
                        <p>{t('loadError')}</p>
                        <p className="text-sm">{t('checkAdmin')}</p>
                    </div>
                    <Button onClick={() => refetch()} variant="outline" size="sm">
                        <RotateCcw className="mr-2 h-4 w-4" /> {tCommon('buttons.retry')}
                    </Button>
                </CardContent>
            </Card>
        );
    }

    // Fallback for empty data (shouldn't happen on success, but just in case)
    if (!data) return null;

    const allServicesUp = data.services.every(s => s.status === "UP");

    return (
        <Card className="col-span-full border-t-4 border-t-blue-500 animate-in fade-in duration-500">
            <CardHeader>
                <div className="flex items-center justify-between">
                    <div>
                        <CardTitle className="flex items-center gap-2">
                            <Activity className="h-5 w-5 text-blue-500" />
                            {t('title')}
                        </CardTitle>
                        <CardDescription>{t('adminDescription')}</CardDescription>
                    </div>
                    <Badge
                        variant="outline"
                        className={`
                    ${allServicesUp ? 'text-green-600 bg-green-50 border-green-200' : 'text-red-600 bg-red-50 border-red-200'}
                `}
                    >
                        {allServicesUp ? t('allSystemsUp') : t('serviceOutage')}
                    </Badge>
                </div>
            </CardHeader>
            <CardContent className="space-y-6">

                {/* Service Health Cards */}
                <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
                    {data.services.map((service: any) => (
                        <div key={service.name} className="flex flex-col items-center p-3 border rounded-lg bg-card hover:bg-accent/50 transition-colors">
                            <div className="flex items-center gap-2 mb-2">
                                <Server className="h-4 w-4 text-muted-foreground" />
                                <span className="text-sm font-medium">{service.name}</span>
                            </div>
                            <div className="flex items-center gap-1.5">
                                <div className={`h-2.5 w-2.5 rounded-full ${service.status === 'UP' ? 'bg-green-500 animate-pulse' : 'bg-red-500'}`} />
                                <span className={`text-xs font-bold ${service.status === 'UP' ? 'text-green-600' : 'text-red-600'}`}>
                                    {service.status === 'UP' ? t('running') : t('down')}
                                </span>
                            </div>
                            {service.message && (
                                <span className="text-[10px] text-muted-foreground mt-1 text-center truncate w-full" title={service.message}>
                                    {service.message}
                                </span>
                            )}
                        </div>
                    ))}
                </div>

                <div className="grid md:grid-cols-2 gap-6">
                    {/* Resource Usage */}
                    <div className="space-y-4">
                        <h4 className="text-sm font-semibold flex items-center gap-2">
                            <Cpu className="h-4 w-4" /> {t('resourceUsage')}
                        </h4>
                        <div className="space-y-3">
                            <div>
                                <div className="flex justify-between text-xs mb-1">
                                    <span>JVM Heap</span>
                                    <span className="text-muted-foreground">
                                        {Math.round((data.resources.jvmHeapUsage / data.resources.jvmHeapMax) * 100)}%
                                        ({Math.round(data.resources.jvmHeapUsage / 1024 / 1024)}MB / {Math.round(data.resources.jvmHeapMax / 1024 / 1024)}MB)
                                    </span>
                                </div>
                                <div className="h-2 w-full bg-secondary rounded-full overflow-hidden">
                                    <div
                                        className={`h-full ${(data.resources.jvmHeapUsage / data.resources.jvmHeapMax) > 0.85 ? 'bg-red-500' : 'bg-blue-500'
                                            }`}
                                        style={{ width: `${Math.min(100, (data.resources.jvmHeapUsage / data.resources.jvmHeapMax) * 100)}%` }}
                                    />
                                </div>
                            </div>
                            <div>
                                <div className="flex justify-between text-xs mb-1">
                                    <span>DB Bağlantıları</span>
                                    <span className="text-muted-foreground">
                                        {data.resources.dbActiveConnections} / {data.resources.dbMaxConnections}
                                    </span>
                                </div>
                                <div className="h-2 w-full bg-secondary rounded-full overflow-hidden">
                                    <div
                                        className="h-full bg-green-500"
                                        style={{ width: `${Math.min(100, (data.resources.dbActiveConnections / data.resources.dbMaxConnections) * 100)}%` }}
                                    />
                                </div>
                            </div>
                            <div>
                                <div className="flex justify-between text-xs mb-1">
                                    <span>Disk Alanı</span>
                                    <span className="text-muted-foreground">
                                        {Math.round((data.resources.diskUsage / data.resources.diskTotal) * 100)}%
                                        ({Math.round(data.resources.diskUsage / 1024 / 1024 / 1024)}GB / {Math.round(data.resources.diskTotal / 1024 / 1024 / 1024)}GB)
                                    </span>
                                </div>
                                <div className="h-2 w-full bg-secondary rounded-full overflow-hidden">
                                    <div
                                        className={`h-full ${(data.resources.diskUsage / data.resources.diskTotal) > 0.8 ? 'bg-red-500' : 'bg-yellow-500'
                                            }`}
                                        style={{ width: `${Math.min(100, (data.resources.diskUsage / data.resources.diskTotal) * 100)}%` }}
                                    />
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Recent Alerts */}
                    <div className="space-y-4">
                        <h4 className="text-sm font-semibold flex items-center gap-2">
                            <AlertTriangle className="h-4 w-4" /> {t('recentAlerts')}
                        </h4>
                        <div className="space-y-2">
                            {data.alerts.length === 0 ? (
                                <div className="flex items-center gap-3 text-sm p-3 rounded bg-green-50 dark:bg-green-900/10 border border-green-100 dark:border-green-900/30">
                                    <CheckCircle2 className="h-5 w-5 text-green-600" />
                                    <span className="text-muted-foreground">{t('noCriticalAlerts')}</span>
                                </div>
                            ) : (
                                <div className="flex flex-col gap-2">
                                    {data.alerts.slice(0, 3).map((alert: any, idx: number) => (
                                        <div key={idx} className={`p-2 rounded text-sm border flex gap-2 items-start ${alert.severity === 'CRITICAL' ? 'bg-red-50 border-red-200 text-red-800' :
                                            alert.severity === 'HIGH' ? 'bg-orange-50 border-orange-200 text-orange-800' :
                                                'bg-yellow-50 border-yellow-200 text-yellow-800'
                                            }`}>
                                            <AlertTriangle className="h-4 w-4 mt-0.5 shrink-0" />
                                            <div className="flex flex-col">
                                                <span className="font-medium">{alert.message}</span>
                                                <span className="text-xs opacity-70">{format.dateTime(new Date(alert.timestamp), { hour: 'numeric', minute: 'numeric', second: 'numeric' })}</span>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            </CardContent>
        </Card>
    );
}

