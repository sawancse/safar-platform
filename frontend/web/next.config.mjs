/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'standalone', // Required for Docker deployment
  images: {
    domains: [
      'localhost',
      'd1234abcdef.cloudfront.net',
      process.env.NEXT_PUBLIC_CDN_DOMAIN ?? '',
    ].filter(Boolean),
  },
  experimental: {
    serverActions: {
      allowedOrigins: ['localhost:3000'],
    },
  },
};

export default nextConfig;
