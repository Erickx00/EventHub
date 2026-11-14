import { Link } from 'react-router-dom'
import type { EventSummary } from '../lib/api'

const dateFormatter = new Intl.DateTimeFormat('pt-BR', {
  day: '2-digit',
  month: 'short',
  year: 'numeric',
})

const timeFormatter = new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' })

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

function metaLine(event: EventSummary): string {
  return [event.city, event.venue].filter(Boolean).join(' · ')
}

export function EventCard({ event }: { event: EventSummary }) {
  const category = categoryLabels[event.category] ?? event.category
  const start = new Date(event.startDate)

  return (
    <Link to={`/events/${event.id}`} className="card card-interactive flex flex-col gap-4 p-5">
      <div className="flex items-start justify-between gap-3">
        <span className="chip">{category}</span>
        <div className="text-right">
          <p className="text-sm font-bold text-ink">{dateFormatter.format(start)}</p>
          <p className="text-xs text-ink-faint">{timeFormatter.format(start)}</p>
        </div>
      </div>

      <h2 className="text-lg font-bold leading-snug text-ink">{event.name}</h2>

      {event.description ? (
        <p className="line-clamp-2 text-sm text-ink-soft">{event.description}</p>
      ) : null}

      <p className="mt-auto flex items-center justify-between gap-2 text-sm text-ink-soft">
        <span className="truncate">{metaLine(event)}</span>
        <span className="font-semibold text-brand-700">Ingressos →</span>
      </p>
    </Link>
  )
}
