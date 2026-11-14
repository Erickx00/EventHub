import { useState, type FormEvent } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

interface LocationState {
  readonly from?: string
}

export function LoginPage() {
  const { login, register } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState<'ATTENDEE' | 'ORGANIZER'>('ATTENDEE')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const state = location.state as LocationState | null
  const redirectTo = state?.from ?? '/'

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setBusy(true)
    setError(null)

    try {
      if (mode === 'login') {
        await login(email, password)
      } else {
        await register(name, email, password, role)
      }
      navigate(redirectTo, { replace: true })
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Não foi possível concluir.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="hero-gradient min-h-[calc(100vh-4rem)]">
      <div className="mx-auto w-full max-w-md px-5 py-14">
        <div className="card p-6 sm:p-8">
          <h1 className="text-2xl font-extrabold text-ink">
            {mode === 'login' ? 'Entrar' : 'Criar conta'}
          </h1>
          <p className="mt-1 text-sm text-ink-soft">
            {mode === 'login'
              ? 'Acesse para comprar ingressos e ver suas entradas.'
              : 'Escolha participar ou organizar eventos.'}
          </p>

          <form onSubmit={(event) => void handleSubmit(event)} className="mt-6 flex flex-col gap-4">
            {mode === 'register' ? (
              <>
                <div>
                  <label className="label" htmlFor="name">
                    Nome
                  </label>
                  <input
                    id="name"
                    value={name}
                    onChange={(event) => setName(event.target.value)}
                    required
                    maxLength={120}
                    className="field"
                  />
                </div>

                <div>
                  <label className="label" htmlFor="role">
                    Perfil
                  </label>
                  <select
                    id="role"
                    value={role}
                    onChange={(event) => setRole(event.target.value as 'ATTENDEE' | 'ORGANIZER')}
                    className="field"
                  >
                    <option value="ATTENDEE">Quero participar</option>
                    <option value="ORGANIZER">Quero organizar</option>
                  </select>
                </div>
              </>
            ) : null}

            <div>
              <label className="label" htmlFor="email">
                E-mail
              </label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                required
                autoComplete="email"
                className="field"
              />
            </div>

            <div>
              <label className="label" htmlFor="password">
                Senha
              </label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                required
                minLength={8}
                autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
                className="field"
              />
              {mode === 'register' ? (
                <p className="mt-1 text-xs text-ink-faint">Mínimo de 8 caracteres.</p>
              ) : null}
            </div>

            {error ? (
              <p role="alert" className="rounded-xl bg-amber-50 px-3 py-2 text-sm text-amber-900">
                {error}
              </p>
            ) : null}

            <button type="submit" disabled={busy} className="btn-primary mt-1 w-full">
              {busy ? 'Enviando…' : mode === 'login' ? 'Entrar' : 'Criar conta'}
            </button>

            <button
              type="button"
              onClick={() => {
                setMode(mode === 'login' ? 'register' : 'login')
                setError(null)
              }}
              className="text-sm font-semibold text-brand-700 underline-offset-4 hover:underline"
            >
              {mode === 'login' ? 'Não tenho conta' : 'Já tenho conta'}
            </button>
          </form>
        </div>
      </div>
    </div>
  )
}
