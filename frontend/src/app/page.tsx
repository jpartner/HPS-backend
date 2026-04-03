'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import {
  Sparkles,
  Scissors,
  Heart,
  Paintbrush,
  Eye,
  HandMetal,
  Flower2,
  Droplets,
  Sun,
  Gem,
  Brush,
  Wand2,
  Search,
  Loader2,
  type LucideIcon,
} from 'lucide-react';
import { categoriesApi, type Category } from '@/lib/api';
import { useLanguage } from '@/lib/i18n';

const iconMap: Record<string, LucideIcon> = {
  sparkles: Sparkles,
  scissors: Scissors,
  heart: Heart,
  paintbrush: Paintbrush,
  eye: Eye,
  hand: HandMetal,
  flower: Flower2,
  droplets: Droplets,
  sun: Sun,
  gem: Gem,
  brush: Brush,
  wand: Wand2,
};

function getCategoryIcon(icon: string): LucideIcon {
  const key = icon.toLowerCase();
  for (const [name, component] of Object.entries(iconMap)) {
    if (key.includes(name)) return component;
  }
  return Sparkles;
}

export default function HomePage() {
  const { lang, t } = useLanguage();
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    categoriesApi
      .list(lang)
      .then(setCategories)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [lang]);

  return (
    <div className="flex flex-col min-h-screen">
      {/* Hero Section */}
      <section className="relative bg-gradient-to-br from-rose-500 via-pink-500 to-purple-600 text-white">
        <div className="absolute inset-0 bg-black/10" />
        <div className="relative mx-auto max-w-5xl px-4 py-24 sm:py-32 text-center">
          <h1 className="text-4xl font-bold tracking-tight sm:text-5xl lg:text-6xl">
            {t.hero.title}
          </h1>
          <p className="mt-4 text-lg text-white/90 sm:text-xl max-w-2xl mx-auto">
            {t.hero.subtitle}
          </p>
          <div className="mt-8">
            <Link
              href="#categories"
              className="inline-flex items-center gap-2 rounded-full bg-white px-8 py-3 text-base font-semibold text-rose-600 shadow-lg transition hover:bg-rose-50"
            >
              <Search className="h-5 w-5" />
              {t.hero.cta}
            </Link>
          </div>
        </div>
      </section>

      {/* Categories Grid */}
      <section id="categories" className="mx-auto w-full max-w-5xl px-4 py-16">
        <h2 className="text-2xl font-bold text-gray-900 sm:text-3xl text-center">
          {t.categories.title}
        </h2>
        <p className="mt-2 text-center text-gray-500">
          {t.hero.subtitle}
        </p>

        {loading ? (
          <div className="mt-12 flex justify-center">
            <Loader2 className="h-8 w-8 animate-spin text-rose-400" />
          </div>
        ) : categories.length === 0 ? (
          <p className="mt-12 text-center text-gray-400">
            {t.common.noResults}
          </p>
        ) : (
          <div className="mt-10 grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
            {categories.map((category) => {
              const Icon = getCategoryIcon(category.icon);
              const href = category.slug
                ? `/categories/${category.slug}`
                : `/categories/${category.id}`;
              return (
                <Link
                  key={category.id}
                  href={href}
                  className="group relative overflow-hidden rounded-2xl border border-gray-100 shadow-sm transition hover:border-rose-200 hover:shadow-md"
                >
                  {category.imageUrl ? (
                    <div className="relative h-40">
                      <img
                        src={category.imageUrl}
                        alt={category.name}
                        className="h-full w-full object-cover transition group-hover:scale-105"
                      />
                      <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-black/20 to-transparent" />
                      <div className="absolute bottom-0 left-0 right-0 p-4">
                        <div className="flex items-center gap-2">
                          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-white/20 backdrop-blur-sm text-white">
                            <Icon className="h-4 w-4" />
                          </div>
                          <span className="text-sm font-semibold text-white drop-shadow">
                            {category.name}
                          </span>
                        </div>
                      </div>
                    </div>
                  ) : (
                    <div className="flex flex-col items-center gap-3 bg-white p-6">
                      <div className="flex h-14 w-14 items-center justify-center rounded-full bg-rose-50 text-rose-500 transition group-hover:bg-rose-100">
                        <Icon className="h-7 w-7" />
                      </div>
                      <span className="text-sm font-medium text-gray-700 text-center group-hover:text-rose-600">
                        {category.name}
                      </span>
                    </div>
                  )}
                </Link>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
}
