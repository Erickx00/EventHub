import { Link } from 'react-router-dom'
import { StatusBanner } from '../components/StatusBanner'
import { EventsPage } from './EventsPage'

export function HomePage() {
  return (
    <>
      <section className="hero-gradient border-b border-brand-700/10">
        <div className="mx-auto max-w-6xl px-5 pb-24 pt-12 sm:pt-16">
          <div className="flex flex-wrap items-center gap-3">
            <span className="chip">Plataforma de eventos</span>
            <StatusBanner />
          </div>

          <h1 className="mt-5 max-w-3xl text-4xl font-extrabold text-ink sm:text-5xl lg:text-6xl">
            Descubra o próximo evento que você não vai querer perder
          </h1>

          <p className="mt-4 max-w-xl text-base text-ink-soft sm:text-lg">
            Compre em minutos e apresente o QR Code na entrada. Organize, publique e acompanhe as
            vendas do seu evento em um painel só.
          </p>

          <div className="mt-7 flex flex-wrap gap-3">
            <a href="#eventos" className="btn-primary">
              Ver eventos
            </a>
            <Link to="/organizer" className="btn-secondary">
              Sou organizador
            </Link>
          </div>

          <p className="hero-outline mt-10 select-none text-[19vw] leading-[0.85] sm:text-[9rem]">
            EVENTHUB
          </p>
        </div>
      </section>

      <div id="eventos" className="mx-auto -mt-14 max-w-6xl px-5 pb-16">
        <EventsPage />
      </div>
    </>
  )
}
