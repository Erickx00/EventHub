import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { api, type EventSummary, type TicketTypeSummary } from '../lib/api'

type PageState =
  | { kind: 'loading' }
  | { kind: 'ok'; event: EventSummary; ticketTypes: TicketTypeSummary[] }
  | { kind: 'error'; message: string }

const priceFormatter = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })
const dateFormatter = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'full', timeStyle: 'short' })

const categoryLabels: Record<string, string> = {
  TECHNOLOGY: 'Tecnologia',
  MUSIC: 'Música',
  BUSINESS: 'Negócios',
  SPORTS: 'Esportes',
  ARTS: 'Artes',
  EDUCATION: 'Educação',
  FOOD: 'Gastronomia',
  OTHER: 'Outro',
}

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
        throw new Error('Pagamento não aprovado.')
      }
      navigate('/tickets')
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Não foi possível concluir a compra.')
    } finally {
      setBusy(false)
    }
  }

  if (state.kind === 'loading') {
    return (
      <div className="mx-auto max-w-4xl px-5 py-16">
        <p className="text-sm text-ink-soft">Carregando evento…</p>
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

  const { event, ticketTypes } = state
  const total = ticketTypes.reduce(
    (sum, ticketType) => sum + (quantities[ticketType.id] ?? 0) * ticketType.price,
    0,
  )
  const hasSelection = total > 0

  return (
    <div className="mx-auto max-w-5xl px-5 py-10">
      <div className="grid gap-6 lg:grid-cols-[1.6fr_1fr]">
        <div className="flex flex-col gap-5">
          <header className="hero-gradient card border-0 p-6 sm:p-8">
            <div className="flex flex-wrap items-center gap-2">
              <span className="chip">{categoryLabels[event.category] ?? event.category}</span>
              <span className="chip chip-neutral">{event.status === 'PUBLISHED' ? 'À venda' : event.status}</span>
            </div>

            <h1 className="mt-4 text-3xl font-extrabold text-ink sm:text-4xl">{event.name}</h1>

            <p className="mt-3 text-sm text-ink-soft">
              {dateFormatter.format(new Date(event.startDate))} · {event.city}
              {event.venue ? ` · ${event.venue}` : ''}
            </p>

            {event.description ? (
              <p className="mt-4 max-w-prose text-ink-soft">{event.description}</p>
            ) : null}
          </header>

          <section className="card p-6">
            <h2 className="text-lg font-bold text-ink">Ingressos</h2>

            {ticketTypes.length === 0 ? (
              <p className="mt-3 text-sm text-ink-soft">Este evento ainda não tem ingressos à venda.</p>
            ) : null}

            <div className="mt-4 flex flex-col gap-3">
              {ticketTypes.map((ticketType) => {
                const quantity = quantities[ticketType.id] ?? 0
                const disabled = ticketType.soldOut || ticketType.availableQuantity === 0

                return (
                  <div
                    key={ticketType.id}
                    className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-brand-700/10 bg-brand-50/40 p-4"
                  >
                    <div className="min-w-0">
                      <p className="font-bold text-ink">{ticketType.name}</p>
                      <p className="text-sm text-ink-soft">
                        {priceFormatter.format(ticketType.price)} · {ticketType.availableQuantity} disponíveis ·
                        máx. {ticketType.maxPerOrder} por pedido
                      </p>
                    </div>

                    <div className="flex items-center gap-2">
                      <button
                        type="button"
                        onClick={() => setQuantity(ticketType.id, quantity - 1, ticketType.maxPerOrder)}
                        disabled={disabled || quantity === 0}
                        aria-label={`Remover um ingresso ${ticketType.name}`}
                        className="btn-secondary h-11 w-11 !px-0 text-lg"
                      >
                        −
                      </button>
                      <span className="w-8 text-center font-bold text-ink" aria-live="polite">
                        {quantity}
                      </span>
                      <button
                        type="button"
                        onClick={() => setQuantity(ticketType.id, quantity + 1, ticketType.maxPerOrder)}
                        disabled={disabled || quantity >= ticketType.maxPerOrder}
                        aria-label={`Adicionar um ingresso ${ticketType.name}`}
                        className="btn-secondary h-11 w-11 !px-0 text-lg"
                      >
                        +
                      </button>
                    </div>
                  </div>
                )
              })}
            </div>
          </section>
        </div>

        <aside className="lg:sticky lg:top-24 lg:self-start">
          <div className="card p-6">
            <p className="text-xs font-bold uppercase tracking-wide text-ink-faint">Resumo</p>
            <p className="mt-2 text-3xl font-extrabold text-ink">{priceFormatter.format(total)}</p>
            <p className="mt-1 text-xs text-ink-soft">
              {hasSelection ? 'Reserva válida por 15 minutos após confirmar.' : 'Selecione ao menos um ingresso.'}
            </p>

            <button
              type="button"
              onClick={() => void handleBuy()}
              disabled={busy || !hasSelection}
              className="btn-primary mt-5 w-full"
            >
              {busy ? 'Processando…' : user ? 'Comprar' : 'Entrar para comprar'}
            </button>

            {error ? (
              <p role="alert" className="mt-3 rounded-xl bg-amber-50 px-3 py-2 text-sm text-amber-900">
                {error}
              </p>
            ) : null}

            <p className="mt-4 text-xs text-ink-faint">
              Pagamento simulado: nenhuma cobrança real é feita neste projeto.
            </p>
          </div>
        </aside>
      </div>
    </div>
  )
}
