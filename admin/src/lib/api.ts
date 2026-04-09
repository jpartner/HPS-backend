// ---------------------------------------------------------------------------
// Admin API client
// ---------------------------------------------------------------------------

const API_BASE =
  (typeof window !== 'undefined'
    ? process.env.NEXT_PUBLIC_API_URL
    : process.env.API_INTERNAL_URL || process.env.NEXT_PUBLIC_API_URL) ||
  'http://localhost:8080';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

export interface Translation {
  title: string;
  description?: string;
}

export interface Translations {
  en?: Translation;
  pl?: Translation;
  uk?: Translation;
  de?: Translation;
  [lang: string]: Translation | undefined;
}

export interface Tenant {
  id: string;
  name: string;
  slug: string;
  domain?: string;
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
  settings?: Record<string, unknown>;
  createdAt: string;
  updatedAt: string;
}

export interface Category {
  id: string;
  tenantId: string;
  parentId?: string | null;
  slug: string;
  icon?: string;
  sortOrder: number;
  active: boolean;
  translations: Translations;
  children?: Category[];
  createdAt: string;
  updatedAt: string;
}

export interface ServiceTemplate {
  id: string;
  tenantId: string;
  categoryId: string;
  slug: string;
  active: boolean;
  translations: Translations;
  attributeIds?: string[];
  createdAt: string;
  updatedAt: string;
}

export interface AttributeDefinition {
  id: string;
  tenantId: string;
  key: string;
  type: 'TEXT' | 'NUMBER' | 'BOOLEAN' | 'SELECT' | 'MULTI_SELECT';
  required: boolean;
  options?: string[];
  translations: Translations;
  createdAt: string;
  updatedAt: string;
}

export interface User {
  id: string;
  email: string;
  firstName?: string;
  lastName?: string;
  role: 'USER' | 'PROVIDER' | 'ADMIN' | 'SUPER_ADMIN';
  status: 'ACTIVE' | 'INACTIVE' | 'BANNED';
  emailVerified: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ProviderProfile {
  id: string;
  userId: string;
  tenantId: string;
  businessName: string;
  verified: boolean;
  verificationStatus: 'PENDING' | 'APPROVED' | 'REJECTED';
  rating?: number;
  reviewCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface ApiKey {
  id: string;
  tenantId: string;
  name: string;
  keyPrefix: string;
  key?: string; // only returned on creation
  active: boolean;
  expiresAt?: string;
  createdAt: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

// ---------------------------------------------------------------------------
// Fetch helper
// ---------------------------------------------------------------------------

function getToken(): string | null {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem('hps_admin_token');
}

export class ApiError extends Error {
  status: number;
  body: unknown;

  constructor(message: string, status: number, body?: unknown) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.body = body;
  }
}

async function request<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const token = getToken();

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> | undefined),
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  // Send selected tenant ID for admin endpoints
  if (typeof window !== 'undefined') {
    const tenantId = localStorage.getItem('hps_admin_tenant');
    if (tenantId) {
      headers['X-Tenant-Id'] = tenantId;
    }
  }

  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  });

  if (!res.ok) {
    let body: unknown;
    try {
      body = await res.json();
    } catch {
      body = await res.text().catch(() => null);
    }
    throw new ApiError(
      `API error ${res.status}: ${res.statusText}`,
      res.status,
      body,
    );
  }

  if (res.status === 204) return undefined as T;

  return res.json() as Promise<T>;
}

function qs(params?: Record<string, string | number | boolean | undefined>): string {
  if (!params) return '';
  const parts: string[] = [];
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined && v !== '') parts.push(`${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`);
  }
  return parts.length ? `?${parts.join('&')}` : '';
}

// ---------------------------------------------------------------------------
// Auth API
// ---------------------------------------------------------------------------

export const authApi = {
  login(data: LoginRequest): Promise<LoginResponse> {
    return request('/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
};

// ---------------------------------------------------------------------------
// Tenant API
// ---------------------------------------------------------------------------

export const tenantApi = {
  list(params?: { page?: number; size?: number; search?: string }) {
    return request<PaginatedResponse<Tenant>>(`/api/v1/admin/tenants${qs(params)}`);
  },
  get(id: string) {
    return request<Tenant>(`/api/v1/admin/tenants/${id}`);
  },
  create(data: Partial<Tenant>) {
    return request<Tenant>('/api/v1/admin/tenants', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
  update(id: string, data: Partial<Tenant>) {
    return request<Tenant>(`/api/v1/admin/tenants/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },
};

// ---------------------------------------------------------------------------
// Category API (admin)
// ---------------------------------------------------------------------------

export interface AdminCategoryDto {
  id: string;
  slug: string | null;
  icon: string | null;
  imageUrl: string | null;
  sortOrder: number;
  parentId: string | null;
  translations: { lang: string; name: string; description?: string }[];
  children: AdminCategoryDto[];
}

export const adminCategoryApi = {
  list() {
    return request<AdminCategoryDto[]>('/api/v1/admin/categories');
  },
  get(id: string) {
    return request<AdminCategoryDto>(`/api/v1/admin/categories/${id}`);
  },
  create(data: {
    slug?: string;
    icon?: string;
    imageUrl?: string;
    sortOrder?: number;
    parentId?: string;
    translations: { lang: string; name: string; description?: string }[];
  }) {
    return request<AdminCategoryDto>('/api/v1/admin/categories', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
  update(id: string, data: {
    slug?: string;
    icon?: string;
    imageUrl?: string;
    sortOrder?: number;
    translations?: { lang: string; name: string; description?: string }[];
  }) {
    return request<AdminCategoryDto>(`/api/v1/admin/categories/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },
  delete(id: string) {
    return request<void>(`/api/v1/admin/categories/${id}`, { method: 'DELETE' });
  },
};

// ---------------------------------------------------------------------------
// Service Template API (admin)
// ---------------------------------------------------------------------------

export interface AdminTemplateDto {
  id: string;
  slug: string;
  categoryId: string;
  defaultDurationMinutes: number | null;
  sortOrder: number;
  isActive: boolean;
  translations: { lang: string; title: string; description?: string }[];
}

export const adminTemplateApi = {
  list() {
    return request<AdminTemplateDto[]>('/api/v1/admin/service-templates');
  },
  get(id: string) {
    return request<AdminTemplateDto>(`/api/v1/admin/service-templates/${id}`);
  },
  create(data: {
    slug: string;
    categoryId: string;
    defaultDurationMinutes?: number | null;
    sortOrder?: number;
    translations: { lang: string; title: string; description?: string }[];
  }) {
    return request<AdminTemplateDto>('/api/v1/admin/service-templates', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
  update(id: string, data: {
    defaultDurationMinutes?: number | null;
    sortOrder?: number;
    isActive?: boolean;
    translations?: { lang: string; title: string; description?: string }[];
  }) {
    return request<AdminTemplateDto>(`/api/v1/admin/service-templates/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },
  delete(id: string) {
    return request<void>(`/api/v1/admin/service-templates/${id}`, { method: 'DELETE' });
  },
};

// ---------------------------------------------------------------------------
// Reference List API (admin)
// ---------------------------------------------------------------------------

export interface ReferenceListItem {
  id?: string;
  value: string;
  sortOrder: number;
  isActive: boolean;
  translations: { lang: string; label: string }[];
}

export interface ReferenceListData {
  id: string;
  key: string;
  name: string;
  isActive: boolean;
  items: ReferenceListItem[];
}

export const adminReferenceListApi = {
  list() {
    return request<ReferenceListData[]>('/api/v1/admin/reference-lists');
  },
  get(id: string) {
    return request<ReferenceListData>(`/api/v1/admin/reference-lists/${id}`);
  },
  create(data: { key: string; name: string; items?: ReferenceListItem[] }) {
    return request<ReferenceListData>('/api/v1/admin/reference-lists', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
  update(id: string, data: { name?: string; isActive?: boolean; items?: ReferenceListItem[] }) {
    return request<ReferenceListData>(`/api/v1/admin/reference-lists/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },
  delete(id: string) {
    return request<void>(`/api/v1/admin/reference-lists/${id}`, { method: 'DELETE' });
  },
};

// ---------------------------------------------------------------------------
// Attribute API (admin)
// ---------------------------------------------------------------------------

export const adminAttributeApi = {
  list(params?: { page?: number; size?: number; search?: string; tenantId?: string }) {
    return request<PaginatedResponse<AttributeDefinition>>(`/api/v1/admin/attributes${qs(params)}`);
  },
  get(id: string) {
    return request<AttributeDefinition>(`/api/v1/admin/attributes/${id}`);
  },
  create(data: Partial<AttributeDefinition>) {
    return request<AttributeDefinition>('/api/v1/admin/attributes', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
  update(id: string, data: Partial<AttributeDefinition>) {
    return request<AttributeDefinition>(`/api/v1/admin/attributes/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },
  delete(id: string) {
    return request<void>(`/api/v1/admin/attributes/${id}`, { method: 'DELETE' });
  },
};

// ---------------------------------------------------------------------------
// User API (admin)
// ---------------------------------------------------------------------------

export const adminUserApi = {
  list(params?: { page?: number; size?: number; search?: string; role?: string; status?: string }) {
    return request<PaginatedResponse<User>>(`/api/v1/admin/users${qs(params)}`);
  },
  get(id: string) {
    return request<User>(`/api/v1/admin/users/${id}`);
  },
  updateStatus(id: string, status: User['status']) {
    return request<User>(`/api/v1/admin/users/${id}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status }),
    });
  },
};

// ---------------------------------------------------------------------------
// Provider API (admin)
// ---------------------------------------------------------------------------

export const adminProviderApi = {
  list(params?: { page?: number; size?: number; search?: string; verificationStatus?: string }) {
    return request<PaginatedResponse<ProviderProfile>>(`/api/v1/admin/providers${qs(params)}`);
  },
  get(id: string) {
    return request<ProviderProfile>(`/api/v1/admin/providers/${id}`);
  },
  verify(id: string, data: { verificationStatus: 'APPROVED' | 'REJECTED'; reason?: string }) {
    return request<ProviderProfile>(`/api/v1/admin/providers/${id}/verify`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
};

// ---------------------------------------------------------------------------
// Country Currency API (admin)
// ---------------------------------------------------------------------------

export interface CountryCurrency {
  countryId: string;
  isoCode: string;
  countryName: string;
  primaryCurrency: string;
  secondaryCurrency?: string | null;
}

export const adminCountryCurrencyApi = {
  list() {
    return request<CountryCurrency[]>('/api/v1/admin/country-currencies');
  },
  upsert(countryId: string, data: { primaryCurrency: string; secondaryCurrency?: string | null }) {
    return request<CountryCurrency>(`/api/v1/admin/country-currencies/${countryId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },
};

// ---------------------------------------------------------------------------
// Rate Duration Preset API (admin)
// ---------------------------------------------------------------------------

export interface RateDurationPreset {
  id: string;
  durationMinutes: number;
  label: string | null;
  sortOrder: number;
  isActive: boolean;
}

export const adminRateDurationPresetApi = {
  list() {
    return request<RateDurationPreset[]>('/api/v1/admin/rate-duration-presets');
  },
  create(data: { durationMinutes: number; label?: string; sortOrder?: number }) {
    return request<RateDurationPreset>('/api/v1/admin/rate-duration-presets', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
  update(id: string, data: { sortOrder?: number; label?: string; isActive?: boolean }) {
    return request<RateDurationPreset>(`/api/v1/admin/rate-duration-presets/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },
  delete(id: string) {
    return request<void>(`/api/v1/admin/rate-duration-presets/${id}`, { method: 'DELETE' });
  },
};

// ---------------------------------------------------------------------------
// Language API (admin)
// ---------------------------------------------------------------------------

export interface LanguageDto {
  code: string;
  name: string;
}

export interface LanguageConfigDto {
  defaultLang: string;
  supportedLangs: LanguageDto[];
}

export const adminLanguageApi = {
  get() {
    return request<LanguageConfigDto>('/api/v1/admin/languages');
  },
  update(data: { defaultLang?: string; supportedLangs: string[] }) {
    return request<LanguageConfigDto>('/api/v1/admin/languages', {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },
};

// ---------------------------------------------------------------------------
// API Key API (admin)
// ---------------------------------------------------------------------------

export const apiKeyApi = {
  list(tenantId: string) {
    return request<ApiKey[]>(`/api/v1/admin/tenants/${tenantId}/api-keys`);
  },
  create(tenantId: string, data: { name: string; expiresAt?: string }) {
    return request<ApiKey>(`/api/v1/admin/tenants/${tenantId}/api-keys`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
  delete(tenantId: string, keyId: string) {
    return request<void>(`/api/v1/admin/tenants/${tenantId}/api-keys/${keyId}`, {
      method: 'DELETE',
    });
  },
};
