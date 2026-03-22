/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'standalone', // Required for Docker deployment
  images: {
    domains: [
      'localhost',
      'd1pei2hufbgxoj.cloudfront.net',
      'media.ysafar.com',
      process.env.NEXT_PUBLIC_CDN_DOMAIN ?? '',
    ].filter(Boolean),
  },
  experimental: {
    missingSuspenseWithCSRBailout: false,
    serverActions: {
      allowedOrigins: ['localhost:3000'],
    },
  },
};

export default nextConfig;
