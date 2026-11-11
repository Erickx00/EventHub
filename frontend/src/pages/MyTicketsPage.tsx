import { useEffect, useState } from 'react'
import { api, type TicketSummary } from '../lib/api'

type PageState =
  | { kind: 'loading' }
  | { kind: 'ok'; tickets: TicketSummary[] }
  | { kind: 'error'; message: string }

const dateFormatter = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'medium', timeStyle: 'short' })

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
    return <p className="text-sm text-slate-400">Carregando ingressos…</p>
  }

  if (state.kind === 'error') {
    return (
      <p role="alert" className="rounded-lg border border-amber-500/30 bg-amber-500/10 p-4 text-sm text-amber-200">
        {state.message}
      </p>
    )
  }

  if (state.tickets.length === 0) {
    return (
      <p className="rounded-lg border border-slate-800 bg-slate-900/40 p-6 text-center text-sm text-slate-400">
        Voce ainda nao tem ingressos. Escolha um evento na pagina inicial.
      </p>
    )
  }

  return (
    <section className="flex flex-col gap-4">
      <h1 className="text-2xl font-semibold text-white">Meus ingressos</h1>

      <div className="grid gap-4 sm:grid-cols-2">
        {state.tickets.map((ticket) => {
          const qrCode = qrCodes[ticket.id]
          const usable = ticket.status === 'VALID'

          return (
            <article
              key={ticket.id}
              className="flex gap-4 rounded-xl border border-slate-800 bg-slate-900/40 p-4"
            >
              {qrCode ? (
                <img
                  src={qrCode}
                  alt={`QR Code do ingresso ${ticket.qrToken.slice(0, 6)}`}
                  className="h-28 w-28 shrink-0 rounded-lg bg-white p-1"
                />
              ) : (
                <div className="flex h-28 w-28 shrink-0 items-center justify-center rounded-lg border border-slate-800 text-xs text-slate-500">
                  sem QR
                </div>
              )}

              <div className="flex flex-col gap-1 text-sm">
                <span
                  className={
                    usable
                      ? 'w-fit rounded-full bg-emerald-500/15 px-2 py-0.5 text-xs font-medium text-emerald-300'
                      : 'w-fit rounded-full bg-slate-700/40 px-2 py-0.5 text-xs font-medium text-slate-300'
                  }
                >
                  {ticket.status}
                </span>
                <span className="text-slate-300">Emitido em {dateFormatter.format(new Date(ticket.issuedAt))}</span>
                <code className="break-all text-xs text-slate-500">{ticket.qrToken}</code>
              </div>
            </article>
          )
        })}
      </div>
    </section>
  )
}
