import React, { useState } from 'react';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { FileText } from 'lucide-react';

interface VerificationLayoutProps {
  documentPanel: React.ReactNode;
  formPanel: React.ReactNode;
}

export const VerificationLayout: React.FC<VerificationLayoutProps> = ({ documentPanel, formPanel }) => {
  const [activeTab, setActiveTab] = useState<'document' | 'form'>('form');

  return (
    <div className="flex flex-col h-[calc(100vh-4rem)] lg:flex-row overflow-hidden">
      {/* Mobile Tab Switcher */}
      <div className="lg:hidden flex border-b border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-950 p-2 gap-2">
        <Button
          variant={activeTab === 'document' ? 'default' : 'ghost'}
          className="flex-1"
          onClick={() => setActiveTab('document')}
        >
          <FileText className="mr-2 h-4 w-4" />
          Belge
        </Button>
        <Button
           variant={activeTab === 'form' ? 'default' : 'ghost'}
          className="flex-1"
          onClick={() => setActiveTab('form')}
        >
          Veriler
        </Button>
      </div>

      <div className={cn(
        "flex-1 overflow-hidden lg:flex lg:w-1/2 border-r border-gray-200 dark:border-gray-800 bg-gray-100 dark:bg-gray-900 relative",
        activeTab === 'document' ? 'flex' : 'hidden'
      )}>
        {documentPanel}
      </div>

      <div className={cn(
        "flex-1 overflow-hidden lg:flex lg:w-1/2 bg-white dark:bg-gray-950 relative",
        activeTab === 'form' ? 'flex' : 'hidden'
      )}>
        {formPanel}
      </div>
    </div>
  );
};
