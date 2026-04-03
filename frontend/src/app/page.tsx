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
  type LucideIcon,
} from 'lucide-react';
import { categoriesApi, type Category } from '@/lib/api';

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

export default async function HomePage() {
  let categories: Category[] = [];
  try {
    categories = await categoriesApi.list('en');
  } catch {
    // Fall through with empty categories
  }

  return (
    <div className="flex flex-col min-h-screen">
      {/* Hero Section */}
      <section className="relative bg-gradient-to-br from-rose-500 via-pink-500 to-purple-600 text-white">
        <div className="absolute inset-0 bg-black/10" />
        <div className="relative mx-auto max-w-5xl px-4 py-24 sm:py-32 text-center">
          <h1 className="text-4xl font-bold tracking-tight sm:text-5xl lg:text-6xl">
            Find your perfect beauty professional
          </h1>
          <p className="mt-4 text-lg text-white/90 sm:text-xl max-w-2xl mx-auto">
            Discover top-rated beauty and wellness providers near you.
            Book appointments with confidence.
          </p>
          <div className="mt-8">
            <Link
              href="#categories"
              className="inline-flex items-center gap-2 rounded-full bg-white px-8 py-3 text-base font-semibold text-rose-600 shadow-lg transition hover:bg-rose-50"
            >
              <Search className="h-5 w-5" />
              Browse Services
            </Link>
          </div>
        </div>
      </section>

      {/* Categories Grid */}
      <section id="categories" className="mx-auto w-full max-w-5xl px-4 py-16">
        <h2 className="text-2xl font-bold text-gray-900 sm:text-3xl text-center">
          Explore Categories
        </h2>
        <p className="mt-2 text-center text-gray-500">
          Choose a category to find the right professional for you
        </p>

        {categories.length === 0 ? (
          <p className="mt-12 text-center text-gray-400">
            No categories available at the moment.
          </p>
        ) : (
          <div className="mt-10 grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
            {categories.map((category) => {
              const Icon = getCategoryIcon(category.icon);
              return (
                <Link
                  key={category.id}
                  href={`/categories/${category.id}`}
                  className="group flex flex-col items-center gap-3 rounded-2xl border border-gray-100 bg-white p-6 shadow-sm transition hover:border-rose-200 hover:shadow-md"
                >
                  <div className="flex h-14 w-14 items-center justify-center rounded-full bg-rose-50 text-rose-500 transition group-hover:bg-rose-100">
                    <Icon className="h-7 w-7" />
                  </div>
                  <span className="text-sm font-medium text-gray-700 text-center group-hover:text-rose-600">
                    {category.name}
                  </span>
                </Link>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
}
