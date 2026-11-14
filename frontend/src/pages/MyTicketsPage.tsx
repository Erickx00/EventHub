import { useEffect, useState } from 'react'
import { api, type TicketSummary } from '../lib/api'

type PageState =
  | { kind: 'loading' }
  | { kind: 'ok'; tickets: TicketSummary[] }
  | { kind: 'error'; message: string }

const dateFormatter = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'medium', timeStyle: 'short' })

const statusLabel: Record<string, string> = {
  VALID: 'Válido',
  USED: 'Utilizado',
  CANCELLED: 'Cancelado',
}

export function MyTicketsPage() {
  const [state, setState] = useState<PageState>({ kind: 'loading' })
  const [qrCodes, setQrCodes] = useState<Record<string, string>>({})

  useEffect(() => {
    let active = true

    api.tickets
      .mine()
      .then((tickets) => {
        if (active) setState({ kind: 'ok', tickets })
      })
      .catch((caught: unknown) => {
        if (!active) return
        setState({ kind: 'error', message: caught instanceof Error ? caught.message : 'Erro ao carregar.' })
      })

    return () => {
      active = false
    }
  }, [])

  useEffect(() => {
    if (state.kind !== 'ok') return

    let active = true
    const createdUrls: string[] = []

    const valid = state.tickets.filter((ticket) => ticket.status === 'VALID')

    void Promise.all(
      valid.map(async (ticket) => {
        const blob = await api.tickets.qrCode(ticket.id)
        const url = URL.createObjectURL(blob)
        createdUrls.push(url)
        return [ticket.id, url] as const
      }),
    )
      .then((entries) => {
        if (!active) return
        setQrCodes(Object.fromEntries(entries))
      })
      .catch(() => {
        // Sem QR o ingresso ainda aparece; o erro real fica visivel na lista.
      })

    return () => {
      active = false
      createdUrls.forEach((url) => URL.revokeObjectURL(url))
    }
  }, [state])

  if (state.kind === 'loading') {
    return (
      <div className="mx-auto max-w-4xl px-5 py-16">
        <p className="text-sm text-ink-soft">Carregando ingressos…</p>
      </div>
    )
  }

  if (state.kind === 'error') {
    return (
      <div className="mx-auto max-w-4xl px-5 py-16">
        <p role="alert" className="card border-amber-200 bg-amber-50 p-5 text-sm text-amber-900">
          {state.message}
        </p>
      </div>
    )
  }

  if (state.tickets.length === 0) {
    return (
      <div className="mx-auto max-w-4xl px-5 py-16">
        <div className="card p-10 text-center">
          <p className="text-lg font-bold text-ink">Você ainda não tem ingressos</p>
          <p className="mt-1 text-sm text-ink-soft">Escolha um evento na página inicial para começar.</p>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-5xl px-5 py-10">
      <h1 className="text-3xl font-extrabold text-ink">Meus ingressos</h1>
      <p className="mt-1 text-sm text-ink-soft">
        Apresente o QR Code na portaria. Cada ingresso vale uma única entrada.
      </p>

      <div className="mt-6 grid gap-5 sm:grid-cols-2">
        {state.tickets.map((ticket) => {
          const qrCode = qrCodes[ticket.id]
          const usable = ticket.status === 'VALID'

          return (
            <article key={ticket.id} className="card flex gap-4 p-5">
              {qrCode ? (
                <img
                  src={qrCode}
                  alt={`QR Code do ingresso ${ticket.qrToken.slice(0, 6)}`}
                  className="h-28 w-28 shrink-0 rounded-xl bg-white p-1 ring-1 ring-brand-700/10"
                />
              ) : (
                <div className="grid h-28 w-28 shrink-0 place-items-center rounded-xl bg-brand-50 text-xs text-ink-faint">
                  sem QR
                </div>
              )}

              <div className="flex min-w-0 flex-col gap-1 text-sm">
                <span className={usable ? 'chip chip-success w-fit' : 'chip chip-neutral w-fit'}>
                  {statusLabel[ticket.status] ?? ticket.status}
                </span>
                <span className="text-ink-soft">Emitido em {dateFormatter.format(new Date(ticket.issuedAt))}</span>
                <code className="mt-auto block truncate text-xs text-ink-faint">{ticket.qrToken}</code>
              </div>
            </article>
          )
        })}
      </div>
    </div>
  )
}
