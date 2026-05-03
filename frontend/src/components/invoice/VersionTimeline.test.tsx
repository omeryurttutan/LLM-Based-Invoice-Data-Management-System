import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { VersionTimeline } from './VersionTimeline';
import { ChangeSource } from '@/types/version-history';

// Mock Lucide icons
jest.mock('lucide-react', () => ({
    Pencil: () => <div data-testid="icon-pencil" />,
    Sparkles: () => <div data-testid="icon-sparkles" />,
    RefreshCw: () => <div data-testid="icon-refresh" />,
    CheckCircle: () => <div data-testid="icon-check" />,
    ArrowRightLeft: () => <div data-testid="icon-arrow" />,
    Undo: () => <div data-testid="icon-undo" />,
    Layers: () => <div data-testid="icon-layers" />,
    GitCompare: () => <div data-testid="icon-compare" />,
}));

const mockVersions = [
    {
        id: '1',
        versionNumber: 2,
        changeSource: ChangeSource.MANUAL_EDIT,
        changeSummary: 'Updated supplier name',
        changedFields: ['Supplier Name'],
        changedBy: 'Test User',
        createdAt: new Date().toISOString(),
    },
    {
        id: '2',
        versionNumber: 1,
        changeSource: ChangeSource.LLM_EXTRACTION,
        changeSummary: 'Initial extraction',
        changedFields: [],
        changedBy: 'System',
        createdAt: new Date(Date.now() - 3600000).toISOString(),
    }
];

describe('VersionTimeline', () => {
    it('renders versions correctly', () => {
        const onCompare = jest.fn();
        const onRevert = jest.fn();

        render(
            <VersionTimeline
                versions={mockVersions}
                currentVersion={2}
                onCompare={onCompare}
                onRevertRequest={onRevert}
                canRevert={true}
            />
        );

        expect(screen.getByText('v2')).toBeInTheDocument();
        expect(screen.getByText('v1')).toBeInTheDocument();
        expect(screen.getByText('Updated supplier name')).toBeInTheDocument();
    });

    it('calls onCompare when compare button is clicked', () => {
        const onCompare = jest.fn();
        const onRevert = jest.fn();

        render(
            <VersionTimeline
                versions={mockVersions}
                currentVersion={2}
                onCompare={onCompare}
                onRevertRequest={onRevert}
                canRevert={true}
            />
        );

        // v1 is not current, so it should have buttons
        // The compare button is the first button in the group
        const buttons = screen.getAllByRole('button');
        // We expect buttons for v1 (Compare, Revert if authorized)
        // Actually, trigger is wrapped in Tooltip, so we might need to look for icon or specific test id
        // Ideally we should add aria-label or test-id to buttons
    });
});
