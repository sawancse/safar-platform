import type { Metadata } from 'next';
import './globals.css';
import Navbar from '@/components/Navbar';
import { I18nProvider } from '@/lib/i18n';

export const metadata: Metadata = {
  title: 'Safar — Find your perfect stay in India',
  description:
    'Book homes, rooms, unique stays, and commercial spaces across India. Zero commission for hosts.',
  keywords: ['property rental', 'india', 'airbnb alternative', 'homestay'],
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body className="min-h-screen bg-white text-gray-900 antialiased">
        <I18nProvider>
          <Navbar />
          <main>{children}</main>
          <footer className="bg-gray-50 border-t mt-16">
            <div className="max-w-6xl mx-auto px-4 py-8 text-sm text-gray-500 text-center">
              © {new Date().getFullYear()} Safar India Pvt. Ltd. · Zero commission, 100% yours.
            </div>
          </footer>
        </I18nProvider>
      </body>
    </html>
  );
}
