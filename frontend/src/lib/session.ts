export interface SessionUser {
  readonly id: string
  readonly name: string
  readonly email: string
  readonly role: 'ATTENDEE' | 'ORGANIZER' | 'ADMIN'
}

export interface StoredSession {
  readonly accessToken: string
  readonly refreshToken: string
  readonly user: SessionUser
}

const KEY = 'eventhub.session'

/**
 * Sessao persistida no localStorage. Guardar o refresh token no navegador e o
 * compromisso pratico de um SPA; a alternativa seria cookie httpOnly, que exige
 * BFF. O token tem prazo curto (15 min) e o refresh e rotativo.
 */
export function loadSession(): StoredSession | null {
  const raw = localStorage.getItem(KEY)
  if (!raw) return null

  try {
    const parsed = JSON.parse(raw) as StoredSession
    if (!parsed.accessToken || !parsed.user) return null
    return parsed
  } catch {
    localStorage.removeItem(KEY)
    return null
  }
}

export function saveSession(session: StoredSession): void {
  localStorage.setItem(KEY, JSON.stringify(session))
}

export function clearSession(): void {
  localStorage.removeItem(KEY)
}

export function accessToken(): string | null {
  return loadSession()?.accessToken ?? null
}
