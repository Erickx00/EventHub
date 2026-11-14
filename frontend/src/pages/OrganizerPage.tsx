import { useEffect, useState, type FormEvent } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { api, type DashboardSummary, type EventSummary } from '../lib/api'

const money = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

const CATEGORIES = ['TECHNOLOGY', 'MUSIC', 'BUSINESS', 'SPORTS', 'ARTS', 'EDUCATION', 'FOOD', 'OTHER'] as const

interface EventForm {
  name: string
  description: string
  category: string
  city: string
  venue: string
  startDate: string
  endDate: string
  ticketName: string
  price: string
  totalQuantity: string
  maxPerOrder: string
}

const emptyForm: EventForm = {
  name: '',
  description: '',
  category: 'TECHNOLOGY',
  city: '',
  venue: '',
  startDate: '',
  endDate: '',
  ticketName: 'Pista',
  price: '50.00',
  totalQuantity: '100',
  maxPerOrder: '4',
}

export function OrganizerPage() {
  const { user } = useAuth()

  const [dashboard, setDashboard] = useState<DashboardSummary | null>(null)
  const [events, setEvents] = useState<EventSummary[]>([])
  const [form, setForm] = useState<EventForm>(emptyForm)
  const [showForm, setShowForm] = useState(false)
  const [publishing, setPublishing] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const allowed = user?.role === 'ORGANIZER' || user?.role === 'ADMIN'

  async function refresh() {
    const [summary, mine] = await Promise.all([api.organizer.dashboard(), api.events.mine()])
    setDashboard(summary)
    setEvents([...mine.content])
  }

  useEffect(() => {
    if (!allowed) return

    let active = true
    void refresh().catch((caught: unknown) => {
      if (active) setError(caught instanceof Error ? caught.message : 'Erro ao carregar.')
    })

    return () => {
      active = false
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [allowed])

  function update<K extends keyof EventForm>(key: K, value: EventForm[K]) {
    setForm((current) => ({ ...current, [key]: value }))
  }

  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setBusy(true)

    try {
      const created = await api.organizer.createEvent({
        name: form.name,
        description: form.description || null,
        category: form.category,
        city: form.city,
        venue: form.venue || null,
        startDate: new Date(form.startDate).toISOString(),
        endDate: new Date(form.endDate).toISOString(),
      })

      // Cria o primeiro tipo de ingresso junto: um evento sem ingresso nao vende.
      await api.organizer.createTicketType(created.id, {
        name: form.ticketName,
        description: null,
        price: Number(form.price),
        totalQuantity: Number(form.totalQuantity),
        maxPerOrder: Number(form.maxPerOrder),
        salesStart: new Date().toISOString(),
        // Vendas encerram no inicio do evento.
        salesEnd: new Date(form.startDate).toISOString(),
      })

      setForm(emptyForm)
      setShowForm(false)
      await refresh()
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Não foi possível criar o evento.')
    } finally {
      setBusy(false)
    }
  }

  async function handlePublish(id: string) {
    setError(null)
    setPublishing(id)
    try {
      await api.organizer.publishEvent(id)
      await refresh()
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Não foi possível publicar.')
    } finally {
      setPublishing(null)
    }
  }

  if (!allowed) {
    return (
      <div className="mx-auto max-w-3xl px-5 py-16">
        <div className="card p-8 text-center">
          <p className="text-lg font-bold text-ink">Área do organizador</p>
          <p className="mt-1 text-sm text-ink-soft">
            Crie uma conta como organizador para publicar eventos.
          </p>
        </div>
      </div>
    )
  }

  const stats = dashboard
    ? [
        { label: 'Eventos', value: String(dashboard.totalEvents) },
        { label: 'Publicados', value: String(dashboard.publishedEvents) },
        { label: 'Vendidos', value: String(dashboard.ticketsSold) },
        { label: 'Disponíveis', value: String(dashboard.ticketsAvailable) },
        { label: 'Receita', value: money.format(dashboard.revenue) },
        { label: 'Check-ins', value: String(dashboard.checkIns) },
      ]
    : []

  return (
    <div className="mx-auto max-w-6xl px-5 py-10">
      <header className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-3xl font-extrabold text-ink">Painel do organizador</h1>
          <p className="mt-1 text-sm text-ink-soft">Acompanhe vendas e publique seus eventos.</p>
        </div>
        <button type="button" onClick={() => setShowForm((current) => !current)} className="btn-primary">
          {showForm ? 'Cancelar' : 'Novo evento'}
        </button>
      </header>

      {dashboard ? (
        <div className="mt-6 grid gap-3 sm:grid-cols-3 lg:grid-cols-6">
          {stats.map((card) => (
            <div key={card.label} className="card p-4">
              <p className="text-[0.7rem] font-bold uppercase tracking-wide text-ink-faint">{card.label}</p>
              <p className="mt-1 text-lg font-extrabold text-ink">{card.value}</p>
            </div>
          ))}
        </div>
      ) : null}

      {showForm ? (
        <form onSubmit={(event) => void handleCreate(event)} className="card mt-6 grid gap-4 p-6 sm:grid-cols-2">
          <div>
            <label className="label" htmlFor="event-name">
              Nome do evento
            </label>
            <input
              id="event-name"
              required
              value={form.name}
              onChange={(event) => update('name', event.target.value)}
              className="field"
            />
          </div>

          <div>
            <label className="label" htmlFor="event-city">
              Cidade
            </label>
            <input
              id="event-city"
              required
              value={form.city}
              onChange={(event) => update('city', event.target.value)}
              className="field"
            />
          </div>

          <div>
            <label className="label" htmlFor="event-category">
              Categoria
            </label>
            <select
              id="event-category"
              value={form.category}
              onChange={(event) => update('category', event.target.value)}
              className="field"
            >
              {CATEGORIES.map((category) => (
                <option key={category} value={category}>
                  {category}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="label" htmlFor="event-venue">
              Local (opcional)
            </label>
            <input
              id="event-venue"
              value={form.venue}
              onChange={(event) => update('venue', event.target.value)}
              className="field"
            />
          </div>

          <div>
            <label className="label" htmlFor="event-start">
              Início
            </label>
            <input
              id="event-start"
              required
              type="datetime-local"
              value={form.startDate}
              onChange={(event) => update('startDate', event.target.value)}
              className="field"
            />
          </div>

          <div>
            <label className="label" htmlFor="event-end">
              Fim
            </label>
            <input
              id="event-end"
              required
              type="datetime-local"
              value={form.endDate}
              onChange={(event) => update('endDate', event.target.value)}
              className="field"
            />
          </div>

          <div className="sm:col-span-2">
            <label className="label" htmlFor="event-description">
              Descrição (opcional)
            </label>
            <textarea
              id="event-description"
              rows={2}
              value={form.description}
              onChange={(event) => update('description', event.target.value)}
              className="field"
            />
          </div>

          <fieldset className="sm:col-span-2 grid gap-4 rounded-2xl border border-brand-700/10 bg-brand-50/40 p-4 sm:grid-cols-4">
            <legend className="px-1 text-[0.7rem] font-bold uppercase tracking-wide text-ink-faint">
              Primeiro tipo de ingresso
            </legend>

            <div>
              <label className="label" htmlFor="ticket-name">
                Nome
              </label>
              <input
                id="ticket-name"
                required
                value={form.ticketName}
                onChange={(event) => update('ticketName', event.target.value)}
                className="field"
              />
            </div>

            <div>
              <label className="label" htmlFor="ticket-price">
                Preço
              </label>
              <input
                id="ticket-price"
                required
                type="number"
                step="0.01"
                min="0"
                value={form.price}
                onChange={(event) => update('price', event.target.value)}
                className="field"
              />
            </div>

            <div>
              <label className="label" htmlFor="ticket-total">
                Quantidade
              </label>
              <input
                id="ticket-total"
                required
                type="number"
                min="1"
                value={form.totalQuantity}
                onChange={(event) => update('totalQuantity', event.target.value)}
                className="field"
              />
            </div>

            <div>
              <label className="label" htmlFor="ticket-max">
                Máx. por pedido
              </label>
              <input
                id="ticket-max"
                required
                type="number"
                min="1"
                value={form.maxPerOrder}
                onChange={(event) => update('maxPerOrder', event.target.value)}
                className="field"
              />
            </div>
          </fieldset>

          <button type="submit" disabled={busy} className="btn-primary sm:col-span-2">
            {busy ? 'Criando…' : 'Criar evento como rascunho'}
          </button>
        </form>
      ) : null}

      {error ? (
        <p role="alert" className="mt-4 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
          {error}
        </p>
      ) : null}

      <section className="mt-8">
        <h2 className="text-lg font-bold text-ink">Meus eventos</h2>

        {events.length === 0 ? (
          <div className="card mt-3 p-8 text-center text-sm text-ink-soft">
            Nenhum evento ainda. Use “Novo evento” para criar o primeiro.
          </div>
        ) : null}

        <div className="mt-3 flex flex-col gap-3">
          {events.map((event) => (
            <div key={event.id} className="card flex flex-wrap items-center justify-between gap-3 p-4">
              <div className="min-w-0">
                <p className="font-bold text-ink">{event.name}</p>
                <p className="text-xs text-ink-soft">
                  {event.city} · {event.status}
                </p>
              </div>

              {event.status === 'DRAFT' ? (
                <button
                  type="button"
                  onClick={() => void handlePublish(event.id)}
                  disabled={publishing === event.id}
                  className="btn-secondary"
                >
                  {publishing === event.id ? 'Publicando…' : 'Publicar'}
                </button>
              ) : (
                <span className="chip chip-neutral">Publicado</span>
              )}
            </div>
          ))}
        </div>
      </section>
    </div>
  )
}
