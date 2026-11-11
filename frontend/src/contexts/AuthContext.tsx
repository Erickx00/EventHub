import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'
import { api, type AuthResponse } from '../lib/api'
import { clearSession, loadSession, saveSession, type SessionUser } from '../lib/session'

interface AuthContextValue {
  readonly user: SessionUser | null
  readonly login: (email: string, password: string) => Promise<void>
  readonly register: (
    name: string,
    email: string,
    password: string,
    role: 'ATTENDEE' | 'ORGANIZER',
  ) => Promise<void>
  readonly logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<SessionUser | null>(() => loadSession()?.user ?? null)

  const applySession = useCallback((auth: AuthResponse) => {
    saveSession({ accessToken: auth.accessToken, refreshToken: auth.refreshToken, user: auth.user })
    setUser(auth.user)
  }, [])

  const login = useCallback(
    async (email: string, password: string) => {
      applySession(await api.auth.login(email, password))
    },
    [applySession],
  )

  const register = useCallback(
    async (name: string, email: string, password: string, role: 'ATTENDEE' | 'ORGANIZER') => {
      applySession(await api.auth.register(name, email, password, role))
    },
    [applySession],
  )

  const logout = useCallback(async () => {
    const session = loadSession()
    if (session) {
      try {
        await api.auth.logout(session.refreshToken)
      } catch {
        // Token ja revogado/expirado: sair localmente e o que importa.
      }
    }
    clearSession()
    setUser(null)
  }, [])

  const value = useMemo(
    () => ({ user, login, register, logout }),
    [user, login, register, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth precisa estar dentro de AuthProvider')
  }
  return context
}
