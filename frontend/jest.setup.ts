import '@testing-library/jest-dom'
import { TextEncoder, TextDecoder } from 'util'
import { TransformStream } from 'stream/web'
import 'whatwg-fetch'

Object.assign(global, { TextEncoder, TextDecoder, TransformStream })

// Polyfill for MSW in JSDOM
// whatwg-fetch adds them to window, we need them on global for MSW interceptors sometimes
if (typeof global.Response === 'undefined') {
    // @ts-ignore
    global.Response = window.Response
    // @ts-ignore
    global.Request = window.Request
    // @ts-ignore
    global.Headers = window.Headers
    // @ts-ignore
    global.fetch = window.fetch
}

// Import server AFTER polyfills
const { server } = require('./src/mocks/server')

beforeAll(() => server.listen())
afterEach(() => {
  server.resetHandlers()
  jest.clearAllMocks()
})
afterAll(() => server.close())

// Mock Next.js navigation
jest.mock('next/navigation', () => ({
  useRouter: () => ({
    push: jest.fn(),
    replace: jest.fn(),
    prefetch: jest.fn(),
    back: jest.fn(),
    refresh: jest.fn(),
  }),
  useSearchParams: () => ({
    get: jest.fn(),
  }),
  usePathname: () => '',
}))
