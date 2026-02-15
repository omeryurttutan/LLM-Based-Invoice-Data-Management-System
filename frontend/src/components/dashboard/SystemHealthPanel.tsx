"use client";

import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Activity, Server, Database, HardDrive, Cpu, AlertTriangle, CheckCircle2 } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";

export function SystemHealthPanel() {
  // Mock data for now as per requirements
  // Phase 40 will implement real data fetching
  const loading = false; 

  if (loading) {
    return (
      <Card className="col-span-full">
        <CardHeader>
          <CardTitle>Sistem Durumu</CardTitle>
          <CardDescription>Sunucu ve servislerin anlık durumu</CardDescription>
        </CardHeader>
        <CardContent className="h-[200px] flex items-center justify-center">
          <Skeleton className="h-[150px] w-full" />
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className="col-span-full border-t-4 border-t-blue-500">
      <CardHeader>
        <div className="flex items-center justify-between">
            <div>
                <CardTitle className="flex items-center gap-2">
                    <Activity className="h-5 w-5 text-blue-500" />
                    Sistem Durumu
                </CardTitle>
                <CardDescription>Sunucu ve servislerin anlık durumu (Admin Paneli)</CardDescription>
            </div>
            <Badge variant="outline" className="text-green-600 bg-green-50 border-green-200">
                Tüm Sistemler Çalışıyor
            </Badge>
        </div>
      </CardHeader>
      <CardContent className="space-y-6">
        
        {/* Service Health Cards */}
        <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
            {[
                { name: "Backend API", status: "UP" },
                { name: "Extraction Svc", status: "UP" },
                { name: "PostgreSQL", status: "UP" },
                { name: "Redis", status: "UP" },
                { name: "RabbitMQ", status: "UP" },
            ].map((service) => (
                <div key={service.name} className="flex flex-col items-center p-3 border rounded-lg bg-card hover:bg-accent/50 transition-colors">
                    <div className="flex items-center gap-2 mb-2">
                        <Server className="h-4 w-4 text-muted-foreground" />
                        <span className="text-sm font-medium">{service.name}</span>
                    </div>
                    <div className="flex items-center gap-1.5">
                        <div className="h-2.5 w-2.5 rounded-full bg-green-500 animate-pulse" />
                        <span className="text-xs font-bold text-green-600">ÇALIŞIYOR</span>
                    </div>
                </div>
            ))}
        </div>

        <div className="grid md:grid-cols-2 gap-6">
            {/* Resource Usage */}
            <div className="space-y-4">
                <h4 className="text-sm font-semibold flex items-center gap-2">
                    <Cpu className="h-4 w-4" /> Kaynak Kullanımı
                </h4>
                <div className="space-y-3">
                    <div>
                        <div className="flex justify-between text-xs mb-1">
                            <span>JVM Heap</span>
                            <span className="text-muted-foreground">45% (2.1GB / 4GB)</span>
                        </div>
                        <div className="h-2 w-full bg-secondary rounded-full overflow-hidden">
                            <div className="h-full bg-blue-500 w-[45%]" />
                        </div>
                    </div>
                    <div>
                        <div className="flex justify-between text-xs mb-1">
                            <span>DB Bağlantıları</span>
                            <span className="text-muted-foreground">22% (11 / 50)</span>
                        </div>
                        <div className="h-2 w-full bg-secondary rounded-full overflow-hidden">
                            <div className="h-full bg-green-500 w-[22%]" />
                        </div>
                    </div>
                    <div>
                        <div className="flex justify-between text-xs mb-1">
                            <span>Disk Alanı</span>
                            <span className="text-muted-foreground">68% (340GB / 500GB)</span>
                        </div>
                        <div className="h-2 w-full bg-secondary rounded-full overflow-hidden">
                            <div className="h-full bg-yellow-500 w-[68%]" />
                        </div>
                    </div>
                </div>
            </div>

            {/* Recent Alerts */}
            <div className="space-y-4">
                <h4 className="text-sm font-semibold flex items-center gap-2">
                    <AlertTriangle className="h-4 w-4" /> Son Alarmlar
                </h4>
                <div className="space-y-2">
                    <div className="flex items-center gap-3 text-sm p-2 rounded bg-green-50/50 dark:bg-green-900/10 border border-green-100 dark:border-green-900/50">
                        <CheckCircle2 className="h-4 w-4 text-green-600" />
                        <span className="text-muted-foreground">Son 24 saatte kritik alarm yok.</span>
                    </div>
                </div>
            </div>
        </div>

      </CardContent>
    </Card>
  );
}
