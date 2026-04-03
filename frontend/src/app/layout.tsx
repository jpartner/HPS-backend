import type { Metadata } from 'next';
import { Geist, Geist_Mono } from 'next/font/google';
import './globals.css';
import { AuthProvider } from '@/lib/auth-context';
import Header from '@/components/Header';

const geistSans = Geist({
  variable: '--font-geist-sans',
  subsets: ['latin'],
});

const geistMono = Geist_Mono({
  variable: '--font-geist-mono',
  subsets: ['latin'],
});

export const metadata: Metadata = {
  title: 'HPS',
  description: 'Find and book the perfect space',
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
        <AuthProvider>
          <Header />
          <main className="flex-1">{children}</main>
          <footer className="border-t border-border bg-white">
            <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-8">
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-8">
                <div>
                  <h3 className="text-lg font-bold text-primary mb-2">HPS</h3>
                  <p className="text-sm text-muted-foreground">
                    Find and book the perfect space for your needs.
                  </p>
                </div>
                <div>
                  <h4 className="text-sm font-semibold text-foreground mb-2">Quick Links</h4>
                  <ul className="space-y-1.5 text-sm text-muted-foreground">
                    <li><a href="/browse" className="hover:text-foreground transition-colors">Browse Spaces</a></li>
                    <li><a href="/bookings" className="hover:text-foreground transition-colors">My Bookings</a></li>
                    <li><a href="/messages" className="hover:text-foreground transition-colors">Messages</a></li>
                  </ul>
                </div>
                <div>
                  <h4 className="text-sm font-semibold text-foreground mb-2">Support</h4>
                  <ul className="space-y-1.5 text-sm text-muted-foreground">
                    <li><a href="/help" className="hover:text-foreground transition-colors">Help Center</a></li>
                    <li><a href="/terms" className="hover:text-foreground transition-colors">Terms of Service</a></li>
                    <li><a href="/privacy" className="hover:text-foreground transition-colors">Privacy Policy</a></li>
                  </ul>
                </div>
              </div>
              <div className="mt-8 pt-6 border-t border-border text-center text-xs text-muted-foreground">
                &copy; {new Date().getFullYear()} HPS. All rights reserved.
              </div>
            </div>
          </footer>
        </AuthProvider>
      </body>
    </html>
  );
}
