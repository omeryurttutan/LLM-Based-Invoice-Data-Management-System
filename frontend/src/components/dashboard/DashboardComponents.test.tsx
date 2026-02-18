import { render, screen } from '@/test-utils/render'
import { SummaryCards } from './SummaryCards'
import { CategoryDistributionChart } from './CategoryDistributionChart'
import { PendingActionsList } from './PendingActionsList'

// Mock Recharts
jest.mock('recharts', () => {
    const OriginalModule = jest.requireActual('recharts');
    return {
        ...OriginalModule,
        ResponsiveContainer: ({ children }: any) => <div className="recharts-responsive-container">{children}</div>,
        PieChart: ({ children }: any) => <div>{children}</div>,
        Pie: () => <div>Pie</div>,
        Cell: () => <div>Cell</div>,
        Tooltip: () => <div>Tooltip</div>,
        Legend: () => <div>Legend</div>,
    };
});

describe('SummaryCards', () => {
    it('renders all stats', () => {
        const stats = {
            totalInvoices: 10,
            pendingCount: 2,
            processedCount: 5,
            rejectedCount: 1,
            totalAmount: 1000,
            currency: 'TRY',
            dailyAverage: 10,
            period: 'this_month',
            summary: { total: 1000, count: 10, average: 100 },
            sourceBreakdown: [],
            confidenceStats: { average: 0.9, min: 0.8, max: 1.0, distribution: [] }
        }
        // @ts-ignore - verify strict types if needed, but for test this suffices if component uses them
        render(<SummaryCards stats={stats} loading={false} currency="TRY" />)
        
        expect(screen.getByText('10')).toBeInTheDocument()
        expect(screen.getByText('2')).toBeInTheDocument()
        expect(screen.getByText('5')).toBeInTheDocument()
    })

    it('renders skeletons when loading', () => {
        // @ts-ignore
        const { container } = render(<SummaryCards stats={undefined} loading={true} currency="TRY" />)
        expect(container.querySelectorAll('.animate-pulse').length).toBeGreaterThan(0)
    })
})

describe('CategoryDistributionChart', () => {
    it('renders chart when data exists', () => {
        const data = [{ 
            categoryId: 1, 
            categoryName: 'Tech', 
            categoryColor: '#000', 
            invoiceCount: 10, 
            totalAmount: 1000, 
            percentage: 100,
            name: 'Tech', // Recharts needs name/value
            value: 1000
        }]
        // @ts-ignore
        render(<CategoryDistributionChart data={data} loading={false} currency="TRY" />)
        expect(screen.getByText('dashboard.charts.categoryDistribution.title')).toBeInTheDocument()
    })
})

describe('PendingActionsList', () => {
    it('renders pending items', () => {
        const invoices = [{ 
            id: '1', 
            invoiceNumber: 'INV-1', 
            supplierName: 'Sup1', 
            totalAmount: 100, 
            currency: 'TRY', 
            status: 'PENDING' as const, 
            invoiceDate: '2024-01-01',
            createdAt: '2024-01-01'
        }]
        const data = { totalPending: 1, invoices }
        // @ts-ignore
        render(<PendingActionsList data={data} loading={false} />)
        
        // Items are in data.invoices
        expect(screen.getByText('INV-1')).toBeInTheDocument()
        expect(screen.getByText('Sup1')).toBeInTheDocument()
    })

    it('renders empty message', () => {
        // @ts-ignore
        render(<PendingActionsList data={{ totalPending: 0, invoices: [] }} loading={false} />)
        expect(screen.getByText('dashboard.pendingActions.empty')).toBeInTheDocument()
    })
})
