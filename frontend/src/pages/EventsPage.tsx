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
      <form
        onSubmit={handleSubmit}
        className="grid gap-3 rounded-xl border border-slate-800 bg-slate-900/40 p-4 sm:grid-cols-[1fr_auto_auto_auto]"
      >
        <label className="sr-only" htmlFor="event-search">
          Buscar por nome
        </label>
        <input
          id="event-search"
          value={term}
          onChange={(event) => setTerm(event.target.value)}
          placeholder="Buscar evento pelo nome"
          className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100 outline-none placeholder:text-slate-500 focus:border-brand-500"
        />

        <label className="sr-only" htmlFor="event-city">
          Cidade
        </label>
        <input
          id="event-city"
          value={city}
          onChange={(event) => setCity(event.target.value)}
          placeholder="Cidade"
          className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100 outline-none placeholder:text-slate-500 focus:border-brand-500"
        />

        <label className="sr-only" htmlFor="event-category">
          Categoria
        </label>
        <select
          id="event-category"
          value={category}
          onChange={(event) => setCategory(event.target.value)}
          className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100 outline-none focus:border-brand-500"
        >
          {CATEGORIES.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>

        <button
          type="submit"
          className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-brand-500"
        >
          Buscar
        </button>
      </form>

      {state.kind === 'loading' ? (
        <p className="text-sm text-slate-400" role="status">
          Carregando eventos…
        </p>
      ) : null}

      {state.kind === 'error' ? (
        <p className="rounded-lg border border-amber-500/30 bg-amber-500/10 p-4 text-sm text-amber-200" role="alert">
          Não foi possível carregar os eventos. {state.message}
          <span className="mt-1 block text-xs text-amber-200/70">API: {apiBaseUrl}</span>
        </p>
      ) : null}

      {state.kind === 'ok' && state.data.content.length === 0 ? (
        <p className="rounded-lg border border-slate-800 bg-slate-900/40 p-6 text-center text-sm text-slate-400">
          Nenhum evento encontrado com esses filtros.
        </p>
      ) : null}

      {state.kind === 'ok' && state.data.content.length > 0 ? (
        <>
          <p className="text-sm text-slate-400">
            {totalElements} {totalElements === 1 ? 'evento encontrado' : 'eventos encontrados'}
          </p>

          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {state.data.content.map((event) => (
              <EventCard key={event.id} event={event} />
            ))}
          </div>

          {totalPages > 1 ? (
            <nav className="flex items-center justify-between" aria-label="Paginação">
              <button
                type="button"
                onClick={() => setPage((current) => Math.max(0, current - 1))}
                disabled={page === 0}
                className="rounded-lg border border-slate-700 px-3 py-2 text-sm text-slate-200 disabled:opacity-40"
              >
                Anterior
              </button>
              <span className="text-sm text-slate-400">
                Página {page + 1} de {totalPages}
              </span>
              <button
                type="button"
                onClick={() => setPage((current) => Math.min(totalPages - 1, current + 1))}
                disabled={page >= totalPages - 1}
                className="rounded-lg border border-slate-700 px-3 py-2 text-sm text-slate-200 disabled:opacity-40"
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
