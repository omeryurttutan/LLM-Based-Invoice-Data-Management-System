import createMiddleware from 'next-intl/middleware';

export default createMiddleware({
  // A list of all locales that are supported
  locales: ['tr', 'en'],

  // Used when no locale matches
  defaultLocale: 'tr',

  // Disable path-based locale prefix (e.g. /tr/path)
  // We want to use cookies to persist locale preference without changing URL
  localePrefix: 'never'
});

export const config = {
  // Match only internationalized pathnames
  matcher: ['/((?!api|_next|_vercel|.*\\..*).*)']
};
