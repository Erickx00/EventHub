import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { api, type EventSummary, type TicketTypeSummary } from '../lib/api'

type PageState =
  | { kind: 'loading' }
  | { kind: 'ok'; event: EventSummary; ticketTypes: TicketTypeSummary[] }
  | { kind: 'error'; message: string }

const priceFormatter = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })
const dateFormatter = new Intl.DateTimeFormat('pt-BR', {
  dateStyle: 'full',
  timeStyle: 'short',
})

export function EventDetailsPage() {
  const { id = '' } = useParams<{ id: string }>()
  const { user } = useAuth()
  const navigate = useNavigate()

  const [state, setState] = useState<PageState>({ kind: 'loading' })
  const [quantities, setQuantities] = useState<Record<string, number>>({})
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    let active = true

    Promise.all([api.events.get(id), api.events.ticketTypes(id)])
      .then(([event, ticketTypes]) => {
        if (active) setState({ kind: 'ok', event, ticketTypes })
      })
      .catch((caught: unknown) => {
        if (!active) return
        setState({ kind: 'error', message: caught instanceof Error ? caught.message : 'Erro ao carregar.' })
      })

    return () => {
      active = false
    }
  }, [id])

  function setQuantity(ticketTypeId: string, quantity: number, max: number) {
    const clamped = Math.max(0, Math.min(quantity, max))
    setQuantities((current) => ({ ...current, [ticketTypeId]: clamped }))
  }

  async function handleBuy() {
    setError(null)

    if (!user) {
      navigate('/login', { state: { from: `/events/${id}` } })
      return
    }

    const items = Object.entries(quantities)
      .filter(([, quantity]) => quantity > 0)
      .map(([ticketTypeId, quantity]) => ({ ticketTypeId, quantity }))

    if (items.length === 0) {
      setError('Escolha ao menos um ingresso.')
      return
    }

    setBusy(true)
    try {
      const order = await api.orders.create(id, items)
      // Simulador: aqui o fluxo real redirecionaria para o gateway.
      const payment = await api.orders.pay(order.id, 'APPROVED')
      if (payment.orderStatus !== 'PAID') {
        throw new Error('Pagamento nao aprovado.')
      }
      navigate('/tickets')
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Nao foi possivel concluir a compra.')
    } finally {
      setBusy(false)
    }
  }

  if (state.kind === 'loading') {
    return <p className="text-sm text-slate-400">Carregando evento…</p>
  }

  if (state.kind === 'error') {
    return (
      <p role="alert" className="rounded-lg border border-amber-500/30 bg-amber-500/10 p-4 text-sm text-amber-200">
        {state.message}
      </p>
    )
  }

  const { event, ticketTypes } = state
  const total = ticketTypes.reduce(
    (sum, ticketType) => sum + (quantities[ticketType.id] ?? 0) * ticketType.price,
    0,
  )

  return (
    <article className="flex flex-col gap-6">
      <header className="flex flex-col gap-2">
        <span className="w-fit rounded-full bg-brand-500/15 px-2.5 py-1 text-xs font-medium text-brand-300">
          {event.category}
        </span>
        <h1 className="text-3xl font-semibold text-white">{event.name}</h1>
        <p className="text-sm text-slate-400">
          {dateFormatter.format(new Date(event.startDate))} · {event.city}
          {event.venue ? ` · ${event.venue}` : ''}
        </p>
        {event.description ? <p className="max-w-prose text-slate-300">{event.description}</p> : null}
      </header>

      <section className="flex flex-col gap-3">
        <h2 className="text-lg font-semibold text-white">Ingressos</h2>

        {ticketTypes.length === 0 ? (
          <p className="rounded-lg border border-slate-800 bg-slate-900/40 p-4 text-sm text-slate-400">
            Este evento ainda nao tem ingressos a venda.
          </p>
        ) : null}

        {ticketTypes.map((ticketType) => {
          const quantity = quantities[ticketType.id] ?? 0
          const disabled = ticketType.soldOut || ticketType.availableQuantity === 0

          return (
            <div
              key={ticketType.id}
              className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-800 bg-slate-900/40 p-4"
            >
              <div>
                <p className="font-medium text-white">{ticketType.name}</p>
                <p className="text-sm text-slate-400">
                  {priceFormatter.format(ticketType.price)} · {ticketType.availableQuantity} disponiveis · max{' '}
                  {ticketType.maxPerOrder} por pedido
                </p>
              </div>

              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => setQuantity(ticketType.id, quantity - 1, ticketType.maxPerOrder)}
                  disabled={disabled || quantity === 0}
                  className="h-9 w-9 rounded-lg border border-slate-700 text-slate-200 disabled:opacity-40"
                >
                  −
                </button>
                <span className="w-8 text-center text-sm text-slate-100">{quantity}</span>
                <button
                  type="button"
                  onClick={() => setQuantity(ticketType.id, quantity + 1, ticketType.maxPerOrder)}
                  disabled={disabled || quantity >= ticketType.maxPerOrder}
                  className="h-9 w-9 rounded-lg border border-slate-700 text-slate-200 disabled:opacity-40"
                >
                  +
                </button>
              </div>
            </div>
          )
        })}
      </section>

      <footer className="flex flex-wrap items-center justify-between gap-3 border-t border-slate-800 pt-4">
        <p className="text-slate-300">
          Total: <span className="font-semibold text-white">{priceFormatter.format(total)}</span>
        </p>

        <button
          type="button"
          onClick={() => void handleBuy()}
          disabled={busy || total === 0}
          className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-brand-500 disabled:opacity-50"
        >
          {busy ? 'Processando…' : user ? 'Comprar' : 'Entrar para comprar'}
        </button>
      </footer>

      {error ? (
        <p role="alert" className="rounded-lg bg-amber-500/10 px-3 py-2 text-sm text-amber-200">
          {error}
        </p>
      ) : null}
    </article>
  )
}
