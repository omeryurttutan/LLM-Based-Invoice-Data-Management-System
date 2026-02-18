import { http, HttpResponse } from 'msw'

export const handlers = [
  // Auth
  http.post('http://localhost:3000/api/v1/auth/login', () => {
    return HttpResponse.json({
      token: 'fake-jwt-token',
      user: { id: 1, name: 'Test User', email: 'test@example.com', role: 'ACCOUNTANT' }
    })
  }),

  http.post('http://localhost:3000/api/v1/auth/register', () => {
    return HttpResponse.json({ message: 'Registration successful' })
  }),

  // Dashboard
  http.get('http://localhost:3000/api/v1/dashboard/stats', () => {
    return HttpResponse.json({
      totalInvoices: 125,
      pendingCount: 12,
      processedCount: 110,
      rejectedCount: 3,
      totalAmount: 450000.50
    })
  }),

  // Invoices
  http.get('http://localhost:3000/api/v1/invoices', ({ request }) => {
    const url = new URL(request.url)
    const page = url.searchParams.get('page') || '0'
    
    return HttpResponse.json({
      content: [
        { id: 1, number: 'INV-001', supplierName: 'Tech Corp', amount: 1500.00, date: '2024-02-15', status: 'VERIFIED' },
        { id: 2, number: 'INV-002', supplierName: 'Office Supplies', amount: 300.50, date: '2024-02-16', status: 'PENDING' },
      ],
      totalPages: 5,
      totalElements: 50,
      number: parseInt(page),
      size: 10
    })
  }),

  http.get('http://localhost:3000/api/v1/invoices/:id', ({ params }) => {
    const { id } = params
    if (id === '999') {
      return new HttpResponse(null, { status: 404 })
    }
    return HttpResponse.json({
      id: Number(id),
      number: 'INV-001',
      supplierName: 'Tech Corp',
      date: '2024-02-15',
      amount: 1500.00,
      status: 'VERIFIED',
      items: [
        { id: 101, description: 'Laptop', quantity: 1, unitPrice: 1500.00, total: 1500.00 }
      ]
    })
  }),

  http.post('http://localhost:3000/api/v1/invoices', () => {
    return HttpResponse.json({ id: 100, message: 'Invoice created' }, { status: 201 })
  }),

  http.put('http://localhost:3000/api/v1/invoices/:id', () => {
    return HttpResponse.json({ id: 100, message: 'Invoice updated' })
  }),

  http.post('http://localhost:3000/api/v1/invoices/:id/verify', () => {
     return HttpResponse.json({ status: 'VERIFIED' })
  }),

  // Categories
  http.get('http://localhost:3000/api/v1/categories', () => {
    return HttpResponse.json([
      { id: 1, name: 'Electronics', code: 'ELEC' },
      { id: 2, name: 'Office', code: 'OFF' }
    ])
  }),

  // Notifications
  http.get('http://localhost:3000/api/v1/notifications/unread-count', () => {
    return HttpResponse.json({ count: 5 })
  }),
  
  http.get('http://localhost:3000/api/v1/notifications', () => {
      return HttpResponse.json({
          content: [
              { id: 1, title: 'New Invoice', message: 'Invoice #123 uploaded', read: false, createdAt: '2024-02-17T10:00:00' }
          ]
      })
  })
]
