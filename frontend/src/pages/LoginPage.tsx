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
      setError(caught instanceof Error ? caught.message : 'Nao foi possivel concluir.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="mx-auto w-full max-w-md">
      <h1 className="mb-1 text-2xl font-semibold text-white">
        {mode === 'login' ? 'Entrar' : 'Criar conta'}
      </h1>
      <p className="mb-6 text-sm text-slate-400">
        {mode === 'login'
          ? 'Acesse para comprar ingressos e ver suas entradas.'
          : 'Escolha participar ou organizar eventos.'}
      </p>

      <form
        onSubmit={(event) => void handleSubmit(event)}
        className="flex flex-col gap-3 rounded-xl border border-slate-800 bg-slate-900/40 p-5"
      >
        {mode === 'register' ? (
          <>
            <label className="text-sm text-slate-300" htmlFor="name">
              Nome
            </label>
            <input
              id="name"
              value={name}
              onChange={(event) => setName(event.target.value)}
              required
              maxLength={120}
              className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100 outline-none focus:border-brand-500"
            />

            <label className="text-sm text-slate-300" htmlFor="role">
              Perfil
            </label>
            <select
              id="role"
              value={role}
              onChange={(event) => setRole(event.target.value as 'ATTENDEE' | 'ORGANIZER')}
              className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100 outline-none focus:border-brand-500"
            >
              <option value="ATTENDEE">Quero participar</option>
              <option value="ORGANIZER">Quero organizar</option>
            </select>
          </>
        ) : null}

        <label className="text-sm text-slate-300" htmlFor="email">
          E-mail
        </label>
        <input
          id="email"
          type="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          required
          autoComplete="email"
          className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100 outline-none focus:border-brand-500"
        />

        <label className="text-sm text-slate-300" htmlFor="password">
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
          className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100 outline-none focus:border-brand-500"
        />

        {error ? (
          <p role="alert" className="rounded-lg bg-amber-500/10 px-3 py-2 text-sm text-amber-200">
            {error}
          </p>
        ) : null}

        <button
          type="submit"
          disabled={busy}
          className="mt-1 rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-brand-500 disabled:opacity-50"
        >
          {busy ? 'Enviando…' : mode === 'login' ? 'Entrar' : 'Criar conta'}
        </button>

        <button
          type="button"
          onClick={() => {
            setMode(mode === 'login' ? 'register' : 'login')
            setError(null)
          }}
          className="text-sm text-slate-400 underline-offset-2 hover:text-slate-200 hover:underline"
        >
          {mode === 'login' ? 'Nao tenho conta' : 'Ja tenho conta'}
        </button>
      </form>
    </section>
  )
}
