/**
 * Cliente HTTP unico da API. Em dev o Vite faz proxy de /api para o backend,
 * entao a base vazia ja funciona; em producao defina VITE_API_URL no build.
 */
const BASE_URL: string = import.meta.env.VITE_API_URL ?? ''

export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${BASE_URL}${path}`, {
    ...init,
    headers: { Accept: 'application/json', ...init?.headers },
  })

  if (!response.ok) {
    throw new ApiError(response.status, `Falha em ${path}: ${response.status} ${response.statusText}`)
  }

  return (await response.json()) as T
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
  events: {
    list: (query: EventQuery = {}) => request<Page<EventSummary>>(`/api/events${toQueryString(query)}`),
  },
}
