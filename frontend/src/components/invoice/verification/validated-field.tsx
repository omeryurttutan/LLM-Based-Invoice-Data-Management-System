import React from 'react';
import { ValidationIssue } from '@/types/invoice';
import { AlertCircle, AlertTriangle, Info } from 'lucide-react';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip';
import { cn } from '@/lib/utils';

interface ValidatedFieldProps {
  children: React.ReactNode;
  issue?: ValidationIssue;
  className?: string;
}

export const ValidatedField: React.FC<ValidatedFieldProps> = ({ children, issue, className }) => {
  if (!issue) {
    return <div className={className}>{children}</div>;
  }

  let Icon = Info;
  let colorClass = 'text-blue-500';
  let bgClass = 'bg-blue-50 dark:bg-blue-900/10';
  let borderColorClass = '';

  if (issue.severity === 'CRITICAL') {
    Icon = AlertCircle;
    colorClass = 'text-red-500';
    bgClass = 'bg-red-50 dark:bg-red-900/10';
    borderColorClass = 'border-red-300 dark:border-red-800';
  } else if (issue.severity === 'WARNING') {
    Icon = AlertTriangle;
    colorClass = 'text-yellow-500';
    bgClass = 'bg-yellow-50 dark:bg-yellow-900/10';
    borderColorClass = 'border-yellow-300 dark:border-yellow-800';
  }

  return (
    <div className={cn("relative group rounded-md transition-colors", bgClass, className)}>
      <div className={cn("relative", issue.severity === 'CRITICAL' || issue.severity === 'WARNING' ? 'ring-1 ring-offset-0 ' + borderColorClass : '')}>
        {children}
      </div>
      
      <div className="absolute right-2 top-1/2 -translate-y-1/2 z-10">
        <TooltipProvider>
          <Tooltip>
            <TooltipTrigger asChild>
              <div className={cn("cursor-help p-1 rounded-full hover:bg-white/50 dark:hover:bg-black/50", colorClass)}>
                <Icon className="h-4 w-4" />
              </div>
            </TooltipTrigger>
            <TooltipContent side="top" className={cn("max-w-xs", colorClass.replace('text-', 'border-').replace('500', '200'))}>
              <p className="font-semibold text-xs mb-1">{issue.severity === 'CRITICAL' ? 'Kritik Hata' : issue.severity === 'WARNING' ? 'Uyarı' : 'Bilgi'}</p>
              <p className="text-sm">{issue.issue}</p>
            </TooltipContent>
          </Tooltip>
        </TooltipProvider>
      </div>
    </div>
  );
};
