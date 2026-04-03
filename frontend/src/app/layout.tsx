import type { Metadata } from 'next';
import { Geist, Geist_Mono } from 'next/font/google';
import './globals.css';
import { AuthProvider } from '@/lib/auth-context';
import { LanguageProvider } from '@/lib/i18n';
import { CountryProvider } from '@/lib/country-context';
import Header from '@/components/Header';
import Footer from '@/components/Footer';

const geistSans = Geist({
  variable: '--font-geist-sans',
  subsets: ['latin', 'latin-ext'],
});

const geistMono = Geist_Mono({
  variable: '--font-geist-mono',
  subsets: ['latin'],
});

export const metadata: Metadata = {
  title: 'HPS - Beauty & Wellness Marketplace',
  description: 'Find and book the best beauty and wellness professionals near you',
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="en"
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
    >
      <body className="min-h-full flex flex-col bg-background text-foreground">
        <LanguageProvider>
          <CountryProvider>
            <AuthProvider>
              <Header />
              <main className="flex-1">{children}</main>
              <Footer />
            </AuthProvider>
          </CountryProvider>
        </LanguageProvider>
      </body>
    </html>
  );
}
