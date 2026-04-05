// ---------------------------------------------------------------------------
// Admin API client
// ---------------------------------------------------------------------------

const API_BASE =
  (typeof window !== 'undefined'
    ? process.env.NEXT_PUBLIC_API_URL
    : process.env.API_INTERNAL_URL || process.env.NEXT_PUBLIC_API_URL) ||
  'http://localhost:8080';

const CLIENT_ID =
  process.env.NEXT_PUBLIC_CLIENT_ID || '11111111-1111-1111-1111-111111111111';
const CLIENT_SECRET =
  process.env.NEXT_PUBLIC_CLIENT_SECRET || 'hps-dev-secret-key';

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
    'X-Client-Id': CLIENT_ID,
    'X-Client-Secret': CLIENT_SECRET,
    ...(options.headers as Record<string, string> | undefined),
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
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

export const adminCategoryApi = {
  list(params?: { page?: number; size?: number; search?: string; tenantId?: string }) {
    return request<PaginatedResponse<Category>>(`/api/v1/admin/categories${qs(params)}`);
  },
  get(id: string) {
    return request<Category>(`/api/v1/admin/categories/${id}`);
  },
  create(data: Partial<Category>) {
    return request<Category>('/api/v1/admin/categories', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
  update(id: string, data: Partial<Category>) {
    return request<Category>(`/api/v1/admin/categories/${id}`, {
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

export const adminTemplateApi = {
  list(params?: { page?: number; size?: number; search?: string; tenantId?: string; categoryId?: string }) {
    return request<PaginatedResponse<ServiceTemplate>>(`/api/v1/admin/service-templates${qs(params)}`);
  },
  get(id: string) {
    return request<ServiceTemplate>(`/api/v1/admin/service-templates/${id}`);
  },
  create(data: Partial<ServiceTemplate>) {
    return request<ServiceTemplate>('/api/v1/admin/service-templates', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
  update(id: string, data: Partial<ServiceTemplate>) {
    return request<ServiceTemplate>(`/api/v1/admin/service-templates/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },
  delete(id: string) {
    return request<void>(`/api/v1/admin/service-templates/${id}`, { method: 'DELETE' });
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
