import { useMemo, useState, type FormEvent } from 'react'
import { EventCard } from '../components/EventCard'
import { useEvents } from '../hooks/useEvents'
import { apiBaseUrl, type EventQuery } from '../lib/api'

const CATEGORIES = [
  { value: '', label: 'Todas as categorias' },
  { value: 'TECHNOLOGY', label: 'Tecnologia' },
  { value: 'MUSIC', label: 'Música' },
  { value: 'BUSINESS', label: 'Negócios' },
  { value: 'SPORTS', label: 'Esportes' },
  { value: 'ARTS', label: 'Artes' },
  { value: 'EDUCATION', label: 'Educação' },
  { value: 'FOOD', label: 'Gastronomia' },
  { value: 'OTHER', label: 'Outro' },
]

const PAGE_SIZE = 9

interface AppliedFilters {
  readonly term: string
  readonly city: string
  readonly category: string
}

const emptyFilters: AppliedFilters = { term: '', city: '', category: '' }

export function EventsPage() {
  const [term, setTerm] = useState('')
  const [city, setCity] = useState('')
  const [category, setCategory] = useState('')
  const [page, setPage] = useState(0)
  const [applied, setApplied] = useState<AppliedFilters>(emptyFilters)

  const query = useMemo<EventQuery>(() => {
    const built: EventQuery = { page, size: PAGE_SIZE }
    if (applied.term) built.q = applied.term
    if (applied.city) built.city = applied.city
    if (applied.category) built.category = applied.category
    return built
  }, [page, applied])

  const state = useEvents(query)

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setPage(0)
    setApplied({ term: term.trim(), city: city.trim(), category })
  }

  const totalPages = state.kind === 'ok' ? state.data.totalPages : 0
  const totalElements = state.kind === 'ok' ? state.data.totalElements : 0

  return (
    <section className="flex flex-col gap-6">
      <form onSubmit={handleSubmit} className="card grid gap-3 p-4 sm:grid-cols-[1.4fr_1fr_auto_auto]">
        <div>
          <label className="label" htmlFor="event-search">
            Buscar
          </label>
          <input
            id="event-search"
            value={term}
            onChange={(event) => setTerm(event.target.value)}
            placeholder="Nome do evento"
            className="field"
          />
        </div>

        <div>
          <label className="label" htmlFor="event-city">
            Cidade
          </label>
          <input
            id="event-city"
            value={city}
            onChange={(event) => setCity(event.target.value)}
            placeholder="Campina Grande"
            className="field"
          />
        </div>

        <div>
          <label className="label" htmlFor="event-category">
            Categoria
          </label>
          <select
            id="event-category"
            value={category}
            onChange={(event) => setCategory(event.target.value)}
            className="field"
          >
            {CATEGORIES.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </div>

        <div className="flex items-end">
          <button type="submit" className="btn-primary w-full sm:w-auto">
            Buscar
          </button>
        </div>
      </form>

      {state.kind === 'loading' ? (
        <p className="text-sm text-ink-soft" role="status">
          Carregando eventos…
        </p>
      ) : null}

      {state.kind === 'error' ? (
        <p role="alert" className="card border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
          Não foi possível carregar os eventos. {state.message}
          <span className="mt-1 block text-xs text-amber-800/70">API: {apiBaseUrl}</span>
        </p>
      ) : null}

      {state.kind === 'ok' && state.data.content.length === 0 ? (
        <div className="card p-10 text-center">
          <p className="text-lg font-bold text-ink">Nenhum evento por aqui ainda</p>
          <p className="mt-1 text-sm text-ink-soft">
            Ajuste os filtros ou volte mais tarde — novos eventos aparecem aqui.
          </p>
        </div>
      ) : null}

      {state.kind === 'ok' && state.data.content.length > 0 ? (
        <>
          <p className="text-sm text-ink-soft">
            {totalElements} {totalElements === 1 ? 'evento encontrado' : 'eventos encontrados'}
          </p>

          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {state.data.content.map((event) => (
              <EventCard key={event.id} event={event} />
            ))}
          </div>

          {totalPages > 1 ? (
            <nav className="flex items-center justify-between gap-3" aria-label="Paginação">
              <button
                type="button"
                onClick={() => setPage((current) => Math.max(0, current - 1))}
                disabled={page === 0}
                className="btn-secondary"
              >
                Anterior
              </button>
              <span className="text-sm text-ink-soft">
                Página {page + 1} de {totalPages}
              </span>
              <button
                type="button"
                onClick={() => setPage((current) => Math.min(totalPages - 1, current + 1))}
                disabled={page >= totalPages - 1}
                className="btn-secondary"
              >
                Próxima
              </button>
            </nav>
          ) : null}
        </>
      ) : null}
    </section>
  )
}
