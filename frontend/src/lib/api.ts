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

export const api = {
  health: () => request<HealthResponse>('/api/health'),
}
