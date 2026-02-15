/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
}

const withNextIntl = require('next-intl/plugin')('./src/i18n.ts');

const withPWA = require('@ducanh2912/next-pwa').default({
  dest: 'public',
  disable: process.env.NODE_ENV === 'development',
  register: true,
  skipWaiting: true,
  cacheOnFrontEndNav: true,
  aggressiveFrontEndNavCaching: true,
  reloadOnOnline: true,
  swcMinify: true,
  swSrc: 'worker/index.js',
  workboxOptions: {
    disableDevLogs: true,
  }
})

module.exports = withPWA(withNextIntl(nextConfig))
