import React from 'react';
import { render, screen } from '@testing-library/react';
import { VersionDiffViewer } from './VersionDiffViewer';
import { ChangeType } from '@/types/version-history';

// Mock Lucide icons
jest.mock('lucide-react', () => ({
    ArrowRight: () => <div data-testid="icon-arrow-right" />,
    ArrowLeft: () => <div data-testid="icon-arrow-left" />,
    ArrowRightLeft: () => <div data-testid="icon-arrow-right-left" />,
    MinusCircle: () => <div data-testid="icon-minus" />,
    PlusCircle: () => <div data-testid="icon-plus" />,
    AlertCircle: () => <div data-testid="icon-alert" />,
}));

const mockDiff = {
    fromVersion: 1,
    toVersion: 2,
    changes: [
        {
            fieldName: 'totalAmount',
            fieldLabel: 'Toplam Tutar',
            oldValue: 100,
            newValue: 200,
            changeType: ChangeType.MODIFIED
        },
        {
            fieldName: 'supplierName',
            fieldLabel: 'Tedarikçi Adı',
            oldValue: 'Old Name',
            newValue: 'New Name',
            changeType: ChangeType.MODIFIED
        }
    ],
    itemChanges: [
        {
            changeType: ChangeType.ADDED,
            description: 'New Item',
            totalAmount: 50
        }
    ]
};

describe('VersionDiffViewer', () => {
    it('renders field changes correctly', () => {
        render(
            <VersionDiffViewer
                diff={mockDiff}
                previousVersionNum={1}
                currentVersionNum={2}
            />
        );

        expect(screen.getByText('Toplam Tutar')).toBeInTheDocument();
        expect(screen.getByText('Old Name')).toBeInTheDocument();
        expect(screen.getByText('New Name')).toBeInTheDocument();
    });

    it('renders item changes correctly', () => {
        render(
            <VersionDiffViewer
                diff={mockDiff}
                previousVersionNum={1}
                currentVersionNum={2}
            />
        );

        expect(screen.getByText('Yeni Kalem Eklendi')).toBeInTheDocument();
    });
});
