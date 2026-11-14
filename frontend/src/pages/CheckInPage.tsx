import { useState, type FormEvent } from 'react'
import { api, ApiError, type CheckInResult } from '../lib/api'

const dateFormatter = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'medium' })

const MESSAGES: Record<string, string> = {
  INVALID_QR_TOKEN: 'Ingresso não encontrado. Confira o código.',
  TICKET_ALREADY_USED: 'Este ingresso já foi utilizado.',
  FORBIDDEN: 'Você não gerencia o evento deste ingresso.',
  EVENT_NOT_FOUND: 'O evento deste ingresso não existe mais.',
}

export function CheckInPage() {
  const [token, setToken] = useState('')
  const [result, setResult] = useState<CheckInResult | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    setResult(null)

    try {
      setResult(await api.checkins.validate(token.trim()))
      setToken('')
    } catch (caught) {
      if (caught instanceof ApiError) {
        setError(MESSAGES[caught.code] ?? caught.message)
      } else {
        setError('Não foi possível validar agora.')
      }
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="hero-gradient min-h-[calc(100vh-4rem)]">
      <div className="mx-auto w-full max-w-xl px-5 py-14">
        <div className="card p-6 sm:p-8">
          <span className="chip">Portaria</span>
          <h1 className="mt-3 text-2xl font-extrabold text-ink">Validar entrada</h1>
          <p className="mt-1 text-sm text-ink-soft">
            Cole o código do ingresso. Cada ingresso vale uma única vez.
          </p>

          <form onSubmit={(event) => void handleSubmit(event)} className="mt-6 flex flex-col gap-4">
            <div>
              <label className="label" htmlFor="qrToken">
                Código do ingresso
              </label>
              <input
                id="qrToken"
                value={token}
                onChange={(event) => setToken(event.target.value)}
                required
                autoFocus
                placeholder="kM3x…"
                className="field font-mono"
              />
            </div>

            <button type="submit" disabled={busy} className="btn-primary w-full">
              {busy ? 'Validando…' : 'Validar entrada'}
            </button>
          </form>

          {result ? (
            <div
              role="status"
              className="mt-5 rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-900"
            >
              <p className="font-bold">Entrada liberada</p>
              <p className="mt-1 text-emerald-800/80">
                Ingresso {result.ticketId.slice(0, 8)} · {dateFormatter.format(new Date(result.checkedAt))}
              </p>
            </div>
          ) : null}

          {error ? (
            <p role="alert" className="mt-5 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
              {error}
            </p>
          ) : null}
        </div>
      </div>
    </div>
  )
}
