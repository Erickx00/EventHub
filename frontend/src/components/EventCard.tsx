import type { EventSummary } from '../lib/api'

const dateFormatter = new Intl.DateTimeFormat('pt-BR', {
  day: '2-digit',
  month: 'short',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

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

export function EventCard({ event }: { event: EventSummary }) {
  const category = categoryLabels[event.category] ?? event.category

  return (
    <article className="flex flex-col gap-3 rounded-xl border border-slate-800 bg-slate-900/60 p-5">
      <div className="flex items-center justify-between gap-2">
        <span className="rounded-full bg-brand-500/15 px-2.5 py-1 text-xs font-medium text-brand-300">
          {category}
        </span>
        <time dateTime={event.startDate} className="text-xs text-slate-400">
          {dateFormatter.format(new Date(event.startDate))}
        </time>
      </div>

      <h2 className="text-lg font-semibold leading-snug text-white">{event.name}</h2>

      {event.description ? (
        <p className="line-clamp-2 text-sm text-slate-400">{event.description}</p>
      ) : null}

      <p className="mt-auto text-sm text-slate-300">
        {event.city}
        {event.venue ? ` · ${event.venue}` : ''}
      </p>
    </article>
  )
}
