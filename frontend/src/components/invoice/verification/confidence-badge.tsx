import React from 'react';
import { cn } from '@/lib/utils';
import { Badge } from '@/components/ui/badge';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip';

interface ConfidenceBadgeProps {
  score: number;
  showLabel?: boolean;
}

export const ConfidenceBadge: React.FC<ConfidenceBadgeProps> = ({ score, showLabel = true }) => {
  let colorClass = '';
  let label = '';
  
  if (score >= 90) {
    colorClass = 'bg-green-100 text-green-800 hover:bg-green-200 dark:bg-green-900/30 dark:text-green-400';
    label = 'Yüksek Güven';
  } else if (score >= 70) {
    colorClass = 'bg-yellow-100 text-yellow-800 hover:bg-yellow-200 dark:bg-yellow-900/30 dark:text-yellow-400';
    label = 'Orta Güven';
  } else {
    colorClass = 'bg-red-100 text-red-800 hover:bg-red-200 dark:bg-red-900/30 dark:text-red-400';
    label = 'Düşük Güven';
  }

  return (
    <TooltipProvider>
      <Tooltip>
        <TooltipTrigger asChild>
          <Badge variant="outline" className={cn("font-medium cursor-help", colorClass)}>
            {score}/100 {showLabel && <span className="ml-1 hidden sm:inline">- {label}</span>}
          </Badge>
        </TooltipTrigger>
        <TooltipContent>
          <p>Güven Skoru: {score} ({label})</p>
        </TooltipContent>
      </Tooltip>
    </TooltipProvider>
  );
};
