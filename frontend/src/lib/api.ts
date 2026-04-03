function getApiBase() {
  if (typeof window === 'undefined' && process.env.API_INTERNAL_URL) {
    return process.env.API_INTERNAL_URL;
  }
  return process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
}

const API_BASE = getApiBase();

interface RequestOptions {
  method?: string;
  body?: unknown;
  token?: string | null;
  lang?: string;
}

export async function api<T = unknown>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, token, lang = 'en' } = options;

  const headers: Record<string, string> = {
    'Accept-Language': lang,
    'Accept': 'application/json',
  };

  if (body) headers['Content-Type'] = 'application/json';
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const res = await fetch(`${API_BASE}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
    cache: 'no-store',
  });

  if (!res.ok) {
    const error = await res.json().catch(() => ({ detail: res.statusText }));
    throw new ApiError(res.status, error.detail || error.title || 'Request failed');
  }

  if (res.status === 204) return undefined as T;

  return res.json();
}

export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

// Auth
export const authApi = {
  register: (data: { email: string; password: string; firstName?: string; lastName?: string; preferredLang?: string }) =>
    api<{ accessToken: string; refreshToken: string; expiresIn: number }>('/api/v1/auth/register', { method: 'POST', body: data }),

  login: (data: { email: string; password: string }) =>
    api<{ accessToken: string; refreshToken: string; expiresIn: number }>('/api/v1/auth/login', { method: 'POST', body: data }),

  refresh: (refreshToken: string) =>
    api<{ accessToken: string; refreshToken: string; expiresIn: number }>('/api/v1/auth/refresh', { method: 'POST', body: { refreshToken } }),
};

// Categories
export const categoriesApi = {
  list: (lang: string) => api<Category[]>('/api/v1/categories', { lang }),
};

// Geo
export const geoApi = {
  countries: (lang: string) => api<Country[]>('/api/v1/countries', { lang }),
  regions: (code: string, lang: string) => api<Region[]>(`/api/v1/countries/${code}/regions`, { lang }),
  cities: (regionId: string, lang: string) => api<City[]>(`/api/v1/regions/${regionId}/cities`, { lang }),
};

// Providers
export const providersApi = {
  list: (params: Record<string, string>, lang: string) => {
    const qs = new URLSearchParams(params).toString();
    return api<{ data: ProviderSummary[]; meta: PageMeta }>(`/api/v1/providers?${qs}`, { lang });
  },
  get: (id: string, lang: string) => api<ProviderDetail>(`/api/v1/providers/${id}`, { lang }),
  availability: (id: string, params: Record<string, string>) => {
    const qs = new URLSearchParams(params).toString();
    return api<Availability>(`/api/v1/providers/${id}/availability?${qs}`);
  },
  createProfile: (data: unknown, token: string) =>
    api<ProviderDetail>('/api/v1/providers/me', { method: 'POST', body: data, token }),
  updateProfile: (data: unknown, token: string) =>
    api<ProviderDetail>('/api/v1/providers/me', { method: 'PUT', body: data, token }),
};

// Services
export const servicesApi = {
  listByProvider: (providerId: string, lang: string) =>
    api<ServiceDto[]>(`/api/v1/providers/${providerId}/services`, { lang }),
  create: (data: unknown, token: string) =>
    api<ServiceDto>('/api/v1/providers/me/services', { method: 'POST', body: data, token }),
  update: (id: string, data: unknown, token: string) =>
    api<ServiceDto>(`/api/v1/providers/me/services/${id}`, { method: 'PUT', body: data, token }),
  delete: (id: string, token: string) =>
    api(`/api/v1/providers/me/services/${id}`, { method: 'DELETE', token }),
};

// Bookings
export const bookingsApi = {
  calculate: (providerId: string, services: { serviceId: string; quantity?: number }[], token: string) =>
    api<BookingCalculation>(`/api/v1/bookings/calculate?providerId=${providerId}`, { method: 'POST', body: services, token }),
  create: (data: unknown, token: string) =>
    api<BookingDto>('/api/v1/bookings', { method: 'POST', body: data, token }),
  list: (token: string) => api<BookingDto[]>('/api/v1/bookings', { token }),
  get: (id: string, token: string) => api<BookingDto>(`/api/v1/bookings/${id}`, { token }),
  quote: (id: string, data: { priceAmount: number; providerNotes?: string }, token: string) =>
    api<BookingDto>(`/api/v1/bookings/${id}/quote`, { method: 'POST', body: data, token }),
  updateStatus: (id: string, data: { status: string; reason?: string }, token: string) =>
    api<BookingDto>(`/api/v1/bookings/${id}/status`, { method: 'PATCH', body: data, token }),
};

// Schedule
export const scheduleApi = {
  getWeekly: (token: string) => api<WeeklySchedule>('/api/v1/providers/me/schedule/weekly', { token }),
  setWeekly: (data: unknown, token: string) =>
    api<WeeklySchedule>('/api/v1/providers/me/schedule/weekly', { method: 'PUT', body: data, token }),
};

// Messaging
export const messagingApi = {
  conversations: (token: string) => api<ConversationDto[]>('/api/v1/conversations', { token }),
  messages: (convId: string, token: string) => api<MessageDto[]>(`/api/v1/conversations/${convId}/messages`, { token }),
  send: (convId: string, content: string, token: string) =>
    api<MessageDto>(`/api/v1/conversations/${convId}/messages`, { method: 'POST', body: { content }, token }),
  create: (participantId: string, initialMessage: string, token: string, topic?: string) =>
    api<ConversationDto>('/api/v1/conversations', { method: 'POST', body: { participantId, initialMessage, topic }, token }),
  markRead: (convId: string, token: string) =>
    api(`/api/v1/conversations/${convId}/read`, { method: 'POST', token }),
  unreadCount: (token: string) => api<{ unread: number }>('/api/v1/messages/unread-count', { token }),
};

// Types
export interface Category { id: string; name: string; icon: string; slug: string | null; imageUrl: string | null; children: Category[]; }
export interface Country { id: string; isoCode: string; phonePrefix: string; name: string; }
export interface Region { id: string; code: string; name: string; latitude: number; longitude: number; }
export interface City { id: string; name: string; latitude: number; longitude: number; population: number; }
export interface PageMeta { page: number; pageSize: number; totalItems: number; }
export interface ProviderSummary {
  id: string; businessName: string; description: string; cityName: string;
  latitude: number; longitude: number; isMobile: boolean; isVerified: boolean;
  avgRating: number; reviewCount: number; categories: { id: string; name: string }[];
  avatarUrl: string | null;
}
export interface GalleryImage { id: string; url: string; caption: string | null; sortOrder: number; }
export interface ProviderDetail extends ProviderSummary {
  email: string; phone: string; areaName: string; addressLine: string;
  serviceRadiusKm: number; services: ServiceDto[];
  galleryImages: GalleryImage[];
}
export interface ServiceDto {
  id: string; title: string; description: string; categoryId: string; categoryName: string;
  pricingType: string; priceAmount: number; priceCurrency: string; durationMinutes: number;
  isActive: boolean; providerId: string; providerName: string;
}
export interface Availability {
  providerId: string; timezone: string; serviceDurationMinutes: number;
  dates: { date: string; slots: string[] }[];
}
export interface BookingCalculation {
  services: { serviceId: string; serviceTitle: string; quantity: number; unitPrice: number; lineTotal: number; durationMinutes: number }[];
  totalAmount: number; totalDurationMinutes: number; currency: string;
}
export interface BookingDto {
  id: string; clientId: string; clientName: string; providerId: string; providerName: string;
  status: string; bookingType: string; scheduledAt: string; totalDurationMinutes: number;
  priceAmount: number; originalAmount: number; priceCurrency: string;
  locationLat: number; locationLng: number; addressText: string;
  clientNotes: string; providerNotes: string;
  services: { serviceId: string; serviceTitle: string; quantity: number; unitPrice: number; lineTotal: number; durationMinutes: number }[];
  createdAt: string; updatedAt: string;
}
export interface WeeklySchedule {
  timezone: string; incallGapMinutes: number; outcallGapMinutes: number; minLeadTimeHours: number;
  slots: { dayOfWeek: number; startTime: string; endTime: string }[];
}
export interface ConversationDto {
  id: string; otherParticipant: { id: string; email: string; name: string; role: string };
  conversationType: string; topic: string; lastMessage: { content: string; senderId: string; createdAt: string; isRead: boolean };
  updatedAt: string;
}
export interface MessageDto {
  id: string; senderId: string; senderName: string; content: string; isRead: boolean; createdAt: string;
}
