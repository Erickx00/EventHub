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
      setError(caught instanceof Error ? caught.message : 'Nao foi possivel criar o evento.')
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
      setError(caught instanceof Error ? caught.message : 'Nao foi possivel publicar.')
    } finally {
      setPublishing(null)
    }
  }

  if (!allowed) {
    return (
      <p className="rounded-lg border border-slate-800 bg-slate-900/40 p-6 text-sm text-slate-400">
        Esta area e para organizadores. Crie uma conta como organizador para publicar eventos.
      </p>
    )
  }

  return (
    <section className="flex flex-col gap-8">
      <header className="flex items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold text-white">Painel do organizador</h1>
        <button
          type="button"
          onClick={() => setShowForm((current) => !current)}
          className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-brand-500"
        >
          {showForm ? 'Cancelar' : 'Novo evento'}
        </button>
      </header>

      {dashboard ? (
        <div className="grid gap-3 sm:grid-cols-3 lg:grid-cols-6">
          {[
            { label: 'Eventos', value: String(dashboard.totalEvents) },
            { label: 'Publicados', value: String(dashboard.publishedEvents) },
            { label: 'Vendidos', value: String(dashboard.ticketsSold) },
            { label: 'Disponiveis', value: String(dashboard.ticketsAvailable) },
            { label: 'Receita', value: money.format(dashboard.revenue) },
            { label: 'Check-ins', value: String(dashboard.checkIns) },
          ].map((card) => (
            <div key={card.label} className="rounded-xl border border-slate-800 bg-slate-900/40 p-4">
              <p className="text-xs uppercase tracking-wide text-slate-500">{card.label}</p>
              <p className="mt-1 text-lg font-semibold text-white">{card.value}</p>
            </div>
          ))}
        </div>
      ) : null}

      {showForm ? (
        <form
          onSubmit={(event) => void handleCreate(event)}
          className="grid gap-3 rounded-xl border border-slate-800 bg-slate-900/40 p-5 sm:grid-cols-2"
        >
          <input
            required
            placeholder="Nome do evento"
            value={form.name}
            onChange={(event) => update('name', event.target.value)}
            className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100"
          />
          <input
            required
            placeholder="Cidade"
            value={form.city}
            onChange={(event) => update('city', event.target.value)}
            className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100"
          />
          <select
            value={form.category}
            onChange={(event) => update('category', event.target.value)}
            className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100"
          >
            {CATEGORIES.map((category) => (
              <option key={category} value={category}>
                {category}
              </option>
            ))}
          </select>
          <input
            placeholder="Local (opcional)"
            value={form.venue}
            onChange={(event) => update('venue', event.target.value)}
            className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100"
          />
          <label className="flex flex-col gap-1 text-xs text-slate-400">
            Inicio
            <input
              required
              type="datetime-local"
              value={form.startDate}
              onChange={(event) => update('startDate', event.target.value)}
              className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100"
            />
          </label>
          <label className="flex flex-col gap-1 text-xs text-slate-400">
            Fim
            <input
              required
              type="datetime-local"
              value={form.endDate}
              onChange={(event) => update('endDate', event.target.value)}
              className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100"
            />
          </label>
          <textarea
            placeholder="Descricao (opcional)"
            value={form.description}
            onChange={(event) => update('description', event.target.value)}
            rows={2}
            className="sm:col-span-2 rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100"
          />

          <fieldset className="sm:col-span-2 grid gap-3 rounded-lg border border-slate-800 p-3 sm:grid-cols-4">
            <legend className="px-1 text-xs uppercase tracking-wide text-slate-500">
              Primeiro tipo de ingresso
            </legend>
            <input
              required
              placeholder="Nome"
              value={form.ticketName}
              onChange={(event) => update('ticketName', event.target.value)}
              className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100"
            />
            <input
              required
              type="number"
              step="0.01"
              min="0"
              placeholder="Preco"
              value={form.price}
              onChange={(event) => update('price', event.target.value)}
              className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100"
            />
            <input
              required
              type="number"
              min="1"
              placeholder="Quantidade"
              value={form.totalQuantity}
              onChange={(event) => update('totalQuantity', event.target.value)}
              className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100"
            />
            <input
              required
              type="number"
              min="1"
              placeholder="Max. por pedido"
              value={form.maxPerOrder}
              onChange={(event) => update('maxPerOrder', event.target.value)}
              className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100"
            />
          </fieldset>

          <button
            type="submit"
            disabled={busy}
            className="sm:col-span-2 rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-brand-500 disabled:opacity-50"
          >
            {busy ? 'Criando…' : 'Criar evento como rascunho'}
          </button>
        </form>
      ) : null}

      {error ? (
        <p role="alert" className="rounded-lg bg-amber-500/10 px-3 py-2 text-sm text-amber-200">
          {error}
        </p>
      ) : null}

      <section className="flex flex-col gap-3">
        <h2 className="text-lg font-semibold text-white">Meus eventos</h2>

        {events.length === 0 ? (
          <p className="rounded-lg border border-slate-800 bg-slate-900/40 p-4 text-sm text-slate-400">
            Nenhum evento ainda. Use "Novo evento" para criar o primeiro.
          </p>
        ) : null}

        {events.map((event) => (
          <div
            key={event.id}
            className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-800 bg-slate-900/40 p-4"
          >
            <div>
              <p className="font-medium text-white">{event.name}</p>
              <p className="text-xs text-slate-400">
                {event.city} · {event.status}
              </p>
            </div>

            {event.status === 'DRAFT' ? (
              <button
                type="button"
                onClick={() => void handlePublish(event.id)}
                disabled={publishing === event.id}
                className="rounded-lg border border-brand-500/60 px-3 py-1.5 text-sm text-brand-300 transition hover:bg-brand-500/10 disabled:opacity-50"
              >
                {publishing === event.id ? 'Publicando…' : 'Publicar'}
              </button>
            ) : (
              <span className="text-xs text-slate-500">Publicado</span>
            )}
          </div>
        ))}
      </section>
    </section>
  )
}
