import { useState, type FormEvent } from 'react'
import { api, ApiError, type CheckInResult } from '../lib/api'

const dateFormatter = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'medium' })

const MESSAGES: Record<string, string> = {
  INVALID_QR_TOKEN: 'Ingresso nao encontrado. Confira o codigo.',
  TICKET_ALREADY_USED: 'Este ingresso ja foi utilizado.',
  FORBIDDEN: 'Voce nao gerencia o evento deste ingresso.',
  EVENT_NOT_FOUND: 'Evento do ingresso nao existe mais.',
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

    const qrToken = token.trim()

    try {
      setResult(await api.checkins.validate(qrToken))
      setToken('')
    } catch (caught) {
      if (caught instanceof ApiError) {
        setError(MESSAGES[caught.code] ?? caught.message)
      } else {
        setError('Nao foi possivel validar agora.')
      }
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="mx-auto w-full max-w-lg">
      <h1 className="text-2xl font-semibold text-white">Portaria</h1>
      <p className="mt-1 mb-6 text-sm text-slate-400">
        Cole o codigo do ingresso para validar a entrada. Cada ingresso vale uma unica vez.
      </p>

      <form
        onSubmit={(event) => void handleSubmit(event)}
        className="flex flex-col gap-3 rounded-xl border border-slate-800 bg-slate-900/40 p-5"
      >
        <label className="text-sm text-slate-300" htmlFor="qrToken">
          Codigo do ingresso
        </label>
        <input
          id="qrToken"
          value={token}
          onChange={(event) => setToken(event.target.value)}
          required
          autoFocus
          placeholder="kM3x..."
          className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 font-mono text-sm text-slate-100 outline-none focus:border-brand-500"
        />

        <button
          type="submit"
          disabled={busy}
          className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-brand-500 disabled:opacity-50"
        >
          {busy ? 'Validando…' : 'Validar entrada'}
        </button>
      </form>

      {result ? (
        <div
          role="status"
          className="mt-4 rounded-xl border border-emerald-500/40 bg-emerald-500/10 p-4 text-sm text-emerald-200"
        >
          <p className="font-medium">Entrada liberada</p>
          <p className="mt-1 text-emerald-100/80">
            Ingresso {result.ticketId.slice(0, 8)} · {dateFormatter.format(new Date(result.checkedAt))}
          </p>
        </div>
      ) : null}

      {error ? (
        <p role="alert" className="mt-4 rounded-xl border border-amber-500/40 bg-amber-500/10 p-4 text-sm text-amber-200">
          {error}
        </p>
      ) : null}
    </section>
  )
}
