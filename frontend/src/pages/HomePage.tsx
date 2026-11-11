import { StatusBanner } from '../components/StatusBanner'
import { EventsPage } from './EventsPage'

export function HomePage() {
  return (
    <section className="flex flex-col gap-8">
      <header className="flex flex-col gap-3">
        <div className="flex items-center gap-3">
          <span className="w-fit rounded-full bg-brand-500/15 px-3 py-1 text-sm font-medium text-brand-300">
            EventHub
          </span>
          <StatusBanner />
        </div>

        <h1 className="text-3xl font-bold tracking-tight text-white">Descubra seu proximo evento</h1>
        <p className="max-w-prose text-slate-400">
          Ingressos para tecnologia, musica, negocios e mais. Filtre por nome, cidade ou categoria.
        </p>
      </header>

      <EventsPage />
    </section>
  )
}
