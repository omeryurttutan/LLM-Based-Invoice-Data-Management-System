import { render, screen, fireEvent } from '@/test-utils/render'
import { UploadZone } from './upload-zone'
import { toast } from 'sonner'

// Mock sonner toast
jest.mock('sonner', () => ({
  toast: {
    error: jest.fn(),
    success: jest.fn()
  }
}))

describe('UploadZone', () => {
  const mockOnFilesSelected = jest.fn()

  beforeEach(() => {
    jest.clearAllMocks()
  })

  it('renders upload zone correctly', () => {
    render(
      <UploadZone 
        onFilesSelected={mockOnFilesSelected} 
        isUploading={false} 
      />
    )
    
    expect(screen.getByText('invoices.upload.zone.dropTitle')).toBeInTheDocument()
    expect(screen.getByText('invoices.upload.zone.supported')).toBeInTheDocument()
  })

  it('handles file selection via input', () => {
    render(
      <UploadZone 
        onFilesSelected={mockOnFilesSelected} 
        isUploading={false} 
      />
    )
    
    const file = new File(['dummy content'], 'test.pdf', { type: 'application/pdf' })
    const input = screen.getByLabelText(/invoices.upload.zone.dropTitle/i, { selector: 'input' }) || document.querySelector('input[type="file"]')
    
    if (input) {
        fireEvent.change(input, { target: { files: [file] } })
        expect(mockOnFilesSelected).toHaveBeenCalledWith([file])
    } else {
        // Fallback if label association is tricky
        const hiddenInput = document.querySelector('input[type="file"]') as HTMLInputElement
        fireEvent.change(hiddenInput, { target: { files: [file] } })
        expect(mockOnFilesSelected).toHaveBeenCalledWith([file])
    }
  })

  it('rejects invalid file extension', () => {
    render(
      <UploadZone 
        onFilesSelected={mockOnFilesSelected} 
        isUploading={false} 
        acceptedExtensions={['.pdf']}
      />
    )
    
    const file = new File(['dummy content'], 'test.txt', { type: 'text/plain' })
    const input = document.querySelector('input[type="file"]') as HTMLInputElement
    
    fireEvent.change(input, { target: { files: [file] } })
    
    expect(mockOnFilesSelected).not.toHaveBeenCalled()
    expect(toast.error).toHaveBeenCalled()
  })

  it('disables input when uploading', () => {
    render(
      <UploadZone 
        onFilesSelected={mockOnFilesSelected} 
        isUploading={true} 
      />
    )
    
    const input = document.querySelector('input[type="file"]') as HTMLInputElement
    expect(input).toBeDisabled()
  })
})
