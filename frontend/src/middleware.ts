import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

// Routes that don't require authentication
const publicRoutes = ['/login', '/register'];

// Routes that require authentication
// We protect everything by default except public routes and static assets
// But listing specific prefixes helps clarify intent

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  
  // Check if route is public
  const isPublicRoute = publicRoutes.some(route => pathname.startsWith(route));
  
  // Get auth token from cookie or localStorage (via cookie)
  // Note: In middleware, we can only access cookies, not localStorage
  // The actual auth check with localStorage happens client-side via AuthProvider
  // However, zustand persist middleware can sync to cookies if configured, 
  // OR we rely on client-side redirection for initial load if cookies aren't used.
  // for this implementation, we will use a hybrid approach:
  // Middleware handles obvious redirects if a cookie exists (optional enhancement),
  // but primarily we rely on AuthProvider for client-side protection if tokens are only in localStorage.
  
  // BUT, to prevent flash of content, it's better if we check something.
  // Since the prompt specifies localStorage for simplicity, middleware has limited visibility.
  // We will implemented basic structure here, but AuthProvider is the real guard.
  
  // Let's check if there is a storage cookie (some zustand persist implementations do this)
  const authCookie = request.cookies.get('auth-storage');
  
  let isAuthenticated = false;
  
  if (authCookie) {
    try {
      const authData = JSON.parse(authCookie.value);
      // Check if state.isAuthenticated is true
      // The structure depends on how zustand persists. Usually { state: { ... }, version: 0 }
      isAuthenticated = authData.state?.isAuthenticated === true;
    } catch {
      isAuthenticated = false;
    }
  }
  
  // Optimization: If we can't read auth state from cookies (localStorage only),
  // we might skip middleware redirection for "protected" routes and let client handle it
  // to avoid false redirects to login for actually logged-in users.
  // HOWEVER, for this phase requirement "Protected routes redirect", we'll implement the logic
  // assuming the client MIGHT sync to cookies or we accept client-side redirect.
  
  // Since we are using localStorage (as per instructions), Middleware can't see it.
  // So valid Middleware protection is tricky without cookies.
  // We will keep the code but comment out the strict "redirect to login" part if no cookie found,
  // to rely on AuthProvider which DOES see localStorage.
  // OR we can just let it pass and AuthProvider cleans up.
  
  // For now, let's just do nothing in middleware regarding auth status if we can't see it,
  // and rely on AuthProvider.
  // UNLESS `auth-storage` cookie is present.
  
  if (authCookie) {
      // Redirect authenticated users away from auth pages
      if (isPublicRoute && isAuthenticated) {
        return NextResponse.redirect(new URL('/', request.url));
      }
      
      // Redirect unauthenticated users to login (if we are sure they are not auth)
      // If cookie exists but isAuthenticated is false.
      if (!isPublicRoute && !isAuthenticated) {
        const loginUrl = new URL('/login', request.url);
        loginUrl.searchParams.set('redirect', pathname);
        return NextResponse.redirect(loginUrl);
      }
  }
  
  return NextResponse.next();
}

export const config = {
  matcher: [
    /*
     * Match all request paths except:
     * - _next/static (static files)
     * - _next/image (image optimization files)
     * - favicon.ico (favicon file)
     * - public folder
     * - api routes
     */
    '/((?!_next/static|_next/image|favicon.ico|public|api).*)',
  ],
};
