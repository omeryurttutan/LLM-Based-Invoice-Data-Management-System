import { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Kimlik Doğrulama | Fatura OCR',
  description: 'Fatura OCR sistemine giriş yapın veya kayıt olun',
};

interface AuthLayoutProps {
  children: React.ReactNode;
}

export default function AuthLayout({ children }: AuthLayoutProps) {
  return (
    <div className="min-h-screen bg-gradient-to-br from-background to-muted relative flex items-center justify-center">
      {/* Optional: Add logo or branding */}
      <div className="absolute top-6 left-6">
        <h1 className="text-xl font-bold text-primary">Fatura OCR</h1>
      </div>
      
      <div className="w-full max-w-md p-4">
        {children}
      </div>
      
      {/* Optional: Footer */}
      <div className="absolute bottom-4 left-0 right-0 text-center text-sm text-muted-foreground">
        © 2025 Fatura OCR ve Veri Yönetim Sistemi
      </div>
    </div>
  );
}
