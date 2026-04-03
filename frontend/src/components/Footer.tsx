'use client';

import { useLanguage } from '@/lib/i18n';

export default function Footer() {
  const { t } = useLanguage();

  return (
    <footer className="border-t border-border bg-white">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-8">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-8">
          <div>
            <h3 className="text-lg font-bold text-primary mb-2">HPS</h3>
            <p className="text-sm text-muted-foreground">
              {t.footer.tagline}
            </p>
          </div>
          <div>
            <h4 className="text-sm font-semibold text-foreground mb-2">
              {t.footer.forClients}
            </h4>
            <ul className="space-y-1.5 text-sm text-muted-foreground">
              <li>
                <a href="/" className="hover:text-foreground transition-colors">
                  {t.footer.browseServices}
                </a>
              </li>
              <li>
                <a href="/bookings" className="hover:text-foreground transition-colors">
                  {t.booking.title}
                </a>
              </li>
            </ul>
          </div>
          <div>
            <h4 className="text-sm font-semibold text-foreground mb-2">
              {t.footer.forProviders}
            </h4>
            <ul className="space-y-1.5 text-sm text-muted-foreground">
              <li>
                <a href="/register" className="hover:text-foreground transition-colors">
                  {t.footer.joinAsProvider}
                </a>
              </li>
              <li>
                <a href="/dashboard" className="hover:text-foreground transition-colors">
                  {t.footer.manageBookings}
                </a>
              </li>
            </ul>
          </div>
        </div>
        <div className="mt-8 pt-6 border-t border-border text-center text-xs text-muted-foreground">
          &copy; {new Date().getFullYear()} HPS. {t.footer.rights}
        </div>
      </div>
    </footer>
  );
}
