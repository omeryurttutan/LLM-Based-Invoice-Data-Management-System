import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function formatCurrency(amount: number, currency: string = 'TRY'): string {
  const locales: Record<string, string> = { TRY: 'tr-TR', USD: 'en-US', EUR: 'de-DE', GBP: 'en-GB' };
  return new Intl.NumberFormat(locales[currency] || 'tr-TR', {
    style: 'currency', currency: currency,
  }).format(amount);
}
