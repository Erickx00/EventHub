import { accessToken, clearSession, type SessionUser } from './session'

/**
 * Cliente HTTP unico da API. Em dev o Vite faz proxy de /api para o backend,
 * entao a base vazia ja funciona; em producao defina VITE_API_URL no build.
 */
const BASE_URL: string = import.meta.env.VITE_API_URL ?? ''

export class ApiError extends Error {
  readonly status: number
  readonly code: string

  constructor(status: number, code: string, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

function authHeaders(): Record<string, string> {
  const token = accessToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}

async function errorFrom(response: Response, path: string): Promise<ApiError> {
  let code = 'HTTP_ERROR'
  let message = `Falha em ${path}: ${response.status}`

  try {
    const body = (await response.json()) as { code?: string; message?: string }
    if (body.code) code = body.code
    if (body.message) message = body.message
  } catch {
    // resposta sem corpo JSON
  }

  return new ApiError(response.status, code, message)
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(`${BASE_URL}${path}`, {
    ...init,
    headers: {
      Accept: 'application/json',
      ...(init.body ? { 'Content-Type': 'application/json' } : {}),
      ...authHeaders(),
      ...init.headers,
    },
  })

  // Token expirado ou revogado: derruba a sessao local para o usuario relogar.
  if (response.status === 401) {
    clearSession()
  }

  if (!response.ok) {
    throw await errorFrom(response, path)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}

async function requestBlob(path: string): Promise<Blob> {
  const response = await fetch(`${BASE_URL}${path}`, { headers: authHeaders() })

  if (response.status === 401) {
    clearSession()
  }
  if (!response.ok) {
    throw await errorFrom(response, path)
  }

  return response.blob()
}

export interface HealthResponse {
  readonly status: string
  readonly service: string
  readonly timestamp: string
}

export interface EventSummary {
  readonly id: string
  readonly name: string
  readonly description: string | null
  readonly category: string
  readonly city: string
  readonly venue: string | null
  readonly startDate: string
  readonly endDate: string
  readonly status: string
}

export interface TicketTypeSummary {
  readonly id: string
  readonly eventId: string
  readonly name: string
  readonly description: string | null
  readonly price: number
  readonly totalQuantity: number
  readonly availableQuantity: number
  readonly maxPerOrder: number
  readonly salesStart: string
  readonly salesEnd: string
  readonly soldOut: boolean
}

export interface Page<T> {
  readonly content: readonly T[]
  readonly page: number
  readonly size: number
  readonly totalElements: number
  readonly totalPages: number
}

export interface EventQuery {
  category?: string
  city?: string
  q?: string
  page?: number
  size?: number
}

export interface AuthResponse {
  readonly tokenType: string
  readonly accessToken: string
  readonly refreshToken: string
  readonly expiresIn: number
  readonly user: SessionUser
}

export interface OrderSummary {
  readonly id: string
  readonly eventId: string
  readonly status: string
  readonly totalAmount: number
  readonly expiresAt: string
  readonly items: readonly { ticketTypeId: string; quantity: number; unitPrice: number; subtotal: number }[]
}

export interface TicketSummary {
  readonly id: string
  readonly eventId: string
  readonly ticketTypeId: string
  readonly status: string
  readonly qrToken: string
  readonly issuedAt: string
}

export interface DashboardSummary {
  readonly totalEvents: number
  readonly publishedEvents: number
  readonly ticketsSold: number
  readonly ticketsAvailable: number
  readonly revenue: number
  readonly checkIns: number
  readonly uniqueAttendees: number
}

export interface CreateEventPayload {
  readonly name: string
  readonly description: string | null
  readonly category: string
  readonly city: string
  readonly venue: string | null
  readonly startDate: string
  readonly endDate: string
}

export interface CreateTicketTypePayload {
  readonly name: string
  readonly description: string | null
  readonly price: number
  readonly totalQuantity: number
  readonly maxPerOrder: number
  readonly salesStart: string
  readonly salesEnd: string
}

function toQueryString(query: EventQuery): string {
  const params = new URLSearchParams()

  if (query.category) params.set('category', query.category)
  if (query.city) params.set('city', query.city)
  if (query.q) params.set('q', query.q)
  if (query.page !== undefined) params.set('page', String(query.page))
  if (query.size !== undefined) params.set('size', String(query.size))

  const serialized = params.toString()
  return serialized ? `?${serialized}` : ''
}

export const api = {
  health: () => request<HealthResponse>('/api/health'),

  auth: {
    login: (email: string, password: string) =>
      request<AuthResponse>('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      }),

    register: (name: string, email: string, password: string, role: 'ATTENDEE' | 'ORGANIZER') =>
      request<AuthResponse>('/api/auth/register', {
        method: 'POST',
        body: JSON.stringify({ name, email, password, role }),
      }),

    logout: (refreshToken: string) =>
      request<void>('/api/auth/logout', { method: 'POST', body: JSON.stringify({ refreshToken }) }),
  },

  events: {
    list: (query: EventQuery = {}) => request<Page<EventSummary>>(`/api/events${toQueryString(query)}`),
    mine: () => request<Page<EventSummary>>('/api/events/mine?size=50'),
    get: (id: string) => request<EventSummary>(`/api/events/${id}`),
    ticketTypes: (id: string) => request<TicketTypeSummary[]>(`/api/events/${id}/ticket-types`),
  },

  organizer: {
    dashboard: () => request<DashboardSummary>('/api/organizer/dashboard'),

    createEvent: (payload: CreateEventPayload) =>
      request<EventSummary>('/api/events', { method: 'POST', body: JSON.stringify(payload) }),

    publishEvent: (id: string) =>
      request<EventSummary>(`/api/events/${id}/publish`, { method: 'PATCH' }),

    createTicketType: (eventId: string, payload: CreateTicketTypePayload) =>
      request<TicketTypeSummary>(`/api/events/${eventId}/ticket-types`, {
        method: 'POST',
        body: JSON.stringify(payload),
      }),
  },

  orders: {
    create: (eventId: string, items: { ticketTypeId: string; quantity: number }[]) =>
      request<OrderSummary>('/api/orders', {
        method: 'POST',
        body: JSON.stringify({ eventId, items }),
      }),

    pay: (orderId: string, outcome: 'APPROVED' | 'REFUSED') =>
      request<{ status: string; orderStatus: string }>(`/api/orders/${orderId}/payments`, {
        method: 'POST',
        body: JSON.stringify({ method: 'PIX', outcome }),
      }),
  },

  tickets: {
    mine: () => request<TicketSummary[]>('/api/tickets/me'),
    qrCode: (id: string) => requestBlob(`/api/tickets/${id}/qr`),
  },
}
