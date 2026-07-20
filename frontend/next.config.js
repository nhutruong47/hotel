/** @type {import('next').NextConfig} */
const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1';

const nextConfig = {
  output: 'standalone',
  reactStrictMode: true,
  images: {
    formats: ['image/avif', 'image/webp'],
  },
  compiler: {
    removeConsole: process.env.NODE_ENV === 'production',
  },
  async rewrites() {
    // Extract origin from API URL for rewrites
    const origin = apiUrl.replace(/\/api\/v1\/?$/, '');
    return [
      {
        source: '/api/:path*',
        destination: `${origin}/api/:path*`,
      },
    ]
  },
}

export default nextConfig
