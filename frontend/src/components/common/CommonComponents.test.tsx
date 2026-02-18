import { render, screen } from '@/test-utils/render'
import { PageHeader } from './page-header'
import { EmptyState } from './empty-state'
import { LoadingSkeleton } from './loading-skeleton'
import { File } from 'lucide-react'

describe('PageHeader', () => {
    it('renders title and description', () => {
        render(<PageHeader title="Test Title" description="Test Description" />)
        expect(screen.getByText('Test Title')).toBeInTheDocument()
        expect(screen.getByText('Test Description')).toBeInTheDocument()
    })
})

describe('EmptyState', () => {
    it('renders empty state content', () => {
        render(<EmptyState title="No Data" description="Add something" icon={File} />)
        expect(screen.getByText('No Data')).toBeInTheDocument()
        expect(screen.getByText('Add something')).toBeInTheDocument()
    })
})

describe('LoadingSkeleton', () => {
    it('renders skeleton', () => {
        const { container } = render(<LoadingSkeleton />)
        expect(container.firstChild).toHaveClass('animate-pulse')
    })
})
