import React from 'react';

interface FormSectionProps {
  title: string;
  children: React.ReactNode;
  rightElement?: React.ReactNode;
}

export const FormSection: React.FC<FormSectionProps> = ({ title, children, rightElement }) => {
  return (
    <div className="space-y-4 p-4 rounded-lg border border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-950 shadow-sm">
      <div className="flex items-center justify-between border-b pb-2 mb-2">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">{title}</h3>
        {rightElement}
      </div>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {children}
      </div>
    </div>
  );
};
