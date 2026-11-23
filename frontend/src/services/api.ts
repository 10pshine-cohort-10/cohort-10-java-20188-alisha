/**
 * Spring Boot REST API Client
 * Connects React frontend to Java Spring Boot backend (/api/v1/*) when VITE_USE_BACKEND_API=true.
 */

import { Contact, EmailEntry, EmailLabel, PhoneEntry, PhoneLabel, User } from '../types';

const API_BASE = import.meta.env.VITE_API_BASE_URL || '';
const TOKEN_KEY = 'cms_jwt_token';

const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null;

const requiredString = (value: unknown, field: string): string => {
  if (typeof value !== 'string') throw new Error(`Invalid API response: ${field} must be a string.`);
  return value;
};

const optionalString = (value: unknown, field: string): string | undefined => {
  if (value === undefined || value === null) return undefined;
  return requiredString(value, field);
};

const requiredNumber = (value: unknown, field: string): number => {
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    throw new Error(`Invalid API response: ${field} must be a number.`);
  }
  return value;
};

const parseEmailLabel = (value: unknown): EmailLabel => {
  if (value === 'work' || value === 'personal' || value === 'other') return value;
  throw new Error('Invalid API response: contact email label is unsupported.');
};

const parsePhoneLabel = (value: unknown): PhoneLabel => {
  if (value === 'work' || value === 'mobile' || value === 'home'
    || value === 'personnel' || value === 'other') return value;
  throw new Error('Invalid API response: contact phone label is unsupported.');
};

const parseCategory = (value: unknown): Contact['category'] => {
  if (value === undefined || value === null || value === 'General') return 'General';
  if (value === 'Clients' || value === 'Partners' || value === 'Vendors' || value === 'Team') {
    return value;
  }
  throw new Error('Invalid API response: contact category is unsupported.');
};

const parseUser = (value: unknown): User => {
  if (!isRecord(value)) throw new Error('Invalid API response: user must be an object.');
  return {
    id: requiredString(value.id, 'user.id'),
    fullName: requiredString(value.fullName, 'user.fullName'),
    email: optionalString(value.email, 'user.email'),
    phone: optionalString(value.phone, 'user.phone'),
    company: optionalString(value.company, 'user.company'),
    role: requiredString(value.role, 'user.role'),
    createdAt: requiredString(value.createdAt, 'user.createdAt'),
    lastLogin: requiredString(value.lastLogin, 'user.lastLogin'),
  };
};

const parseEmail = (value: unknown): EmailEntry => {
  if (!isRecord(value)
    || typeof value.id !== 'string'
    || typeof value.email !== 'string'
    || (value.isPrimary !== undefined && typeof value.isPrimary !== 'boolean')) {
    throw new Error('Invalid API response: contact email is malformed.');
  }
  return {
    id: value.id,
    email: value.email,
    label: parseEmailLabel(value.label),
    isPrimary: value.isPrimary === true,
  };
};

const parsePhone = (value: unknown): PhoneEntry => {
  if (!isRecord(value)
    || typeof value.id !== 'string'
    || typeof value.number !== 'string'
    || (value.isPrimary !== undefined && typeof value.isPrimary !== 'boolean')) {
    throw new Error('Invalid API response: contact phone is malformed.');
  }
  return {
    id: value.id,
    number: value.number,
    label: parsePhoneLabel(value.label),
    isPrimary: value.isPrimary === true,
  };
};

const parseContact = (value: unknown): Contact => {
  if (!isRecord(value) || !Array.isArray(value.emails) || !Array.isArray(value.phones)) {
    throw new Error('Invalid API response: contact is malformed.');
  }
  if (typeof value.isFavorite !== 'boolean') {
    throw new Error('Invalid API response: contact favorite state is malformed.');
  }
  return {
    id: requiredString(value.id, 'contact.id'),
    userId: requiredString(value.userId, 'contact.userId'),
    firstName: requiredString(value.firstName, 'contact.firstName'),
    lastName: requiredString(value.lastName, 'contact.lastName'),
    title: optionalString(value.title, 'contact.title') || '',
    company: optionalString(value.company, 'contact.company') || '',
    department: optionalString(value.department, 'contact.department'),
    emails: value.emails.map(parseEmail),
    phones: value.phones.map(parsePhone),
    address: optionalString(value.address, 'contact.address'),
    city: optionalString(value.city, 'contact.city'),
    state: optionalString(value.state, 'contact.state'),
    country: optionalString(value.country, 'contact.country'),
    notes: optionalString(value.notes, 'contact.notes'),
    category: parseCategory(value.category),
    isFavorite: value.isFavorite,
    avatarColor: optionalString(value.avatarColor, 'contact.avatarColor'),
    createdAt: requiredString(value.createdAt, 'contact.createdAt'),
    updatedAt: requiredString(value.updatedAt, 'contact.updatedAt'),
  };
};

const parseAuthResponse = (value: unknown): { token: string; message: string; user: User } => {
  if (!isRecord(value)) throw new Error('Invalid API response: authentication response is malformed.');
  return {
    token: requiredString(value.token, 'token'),
    message: requiredString(value.message, 'message'),
    user: parseUser(value.user),
  };
};

export const isBackendApiEnabled = (): boolean =>
  import.meta.env.VITE_USE_BACKEND_API === 'true';

export function getAuthToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setAuthToken(token: string | null) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  } else {
    localStorage.removeItem(TOKEN_KEY);
  }
}

async function apiRequest(path: string, options: RequestInit = {}): Promise<unknown> {
  const token = getAuthToken();
  const headers = new Headers(options.headers);
  if (options.body !== undefined && options.body !== null && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    const body: unknown = await response.json().catch(() => null);
    const message = isRecord(body) && typeof body.message === 'string'
      ? body.message
      : `Request failed with status ${response.status}`;
    throw new Error(message);
  }

  if (response.status === 204) return undefined;
  return response.json();
}

export const backendApi = {
  register: async (payload: { fullName: string; identifier: string; password: string; company?: string }) =>
    parseAuthResponse(await apiRequest('/api/v1/auth/register', {
      method: 'POST',
      body: JSON.stringify(payload),
    })),

  login: async (payload: { identifier: string; password: string }) =>
    parseAuthResponse(await apiRequest('/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify(payload),
    })),

  getProfile: async () => parseUser(await apiRequest('/api/v1/users/me')),

  updateProfile: async (payload: { fullName: string; phone?: string; company?: string }) =>
    parseUser(await apiRequest('/api/v1/users/me', {
      method: 'PUT',
      body: JSON.stringify(payload),
    })),

  changePassword: async (payload: { currentPassword: string; newPassword: string }) => {
    const response: unknown = await apiRequest('/api/v1/users/change-password', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
    if (!isRecord(response) || typeof response.message !== 'string') {
      throw new Error('Invalid API response: password update response is malformed.');
    }
    return { message: response.message };
  },

  listContacts: async (params: {
    search?: string;
    category?: string;
    favoriteOnly?: boolean;
    page: number;
    size: number;
    sortBy: string;
    sortDir: string;
  }) => {
    const qs = new URLSearchParams({
      search: params.search || '',
      category: params.category || 'All',
      favoriteOnly: String(Boolean(params.favoriteOnly)),
      page: String(Math.max(0, params.page - 1)),
      size: String(params.size),
      sortBy: params.sortBy,
      sortDir: params.sortDir,
    });
    const response: unknown = await apiRequest(`/api/v1/contacts?${qs.toString()}`);
    if (!isRecord(response) || !Array.isArray(response.contacts)) {
      throw new Error('Invalid API response: contact page is malformed.');
    }
    return {
      contacts: response.contacts.map(parseContact),
      page: requiredNumber(response.page, 'page'),
      pageSize: requiredNumber(response.pageSize, 'pageSize'),
      totalItems: requiredNumber(response.totalItems, 'totalItems'),
      totalPages: requiredNumber(response.totalPages, 'totalPages'),
      totalAll: requiredNumber(response.totalAll, 'totalAll'),
      totalFavorites: requiredNumber(response.totalFavorites, 'totalFavorites'),
    };
  },

  createContact: async (payload: Omit<Contact, 'id' | 'userId' | 'createdAt' | 'updatedAt'>) =>
    parseContact(await apiRequest('/api/v1/contacts', {
      method: 'POST',
      body: JSON.stringify(payload),
    })),

  updateContact: async (
    id: string,
    payload: Omit<Contact, 'id' | 'userId' | 'createdAt' | 'updatedAt'>
  ) => parseContact(await apiRequest(`/api/v1/contacts/${encodeURIComponent(id)}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })),

  toggleFavorite: async (id: string) => {
    const response: unknown = await apiRequest(`/api/v1/contacts/${encodeURIComponent(id)}/favorite`, {
      method: 'PATCH',
    });
    if (!isRecord(response) || typeof response.isFavorite !== 'boolean') {
      throw new Error('Invalid API response: favorite update response is malformed.');
    }
    return { isFavorite: response.isFavorite };
  },

  deleteContact: async (id: string) => {
    await apiRequest(`/api/v1/contacts/${encodeURIComponent(id)}`, {
      method: 'DELETE',
    });
  },

  bulkDelete: async (ids: string[]) => {
    const response: unknown = await apiRequest('/api/v1/contacts/bulk-delete', {
      method: 'POST',
      body: JSON.stringify(ids),
    });
    if (!isRecord(response)) throw new Error('Invalid API response: bulk delete response is malformed.');
    return { deletedCount: requiredNumber(response.deletedCount, 'deletedCount') };
  },
};
