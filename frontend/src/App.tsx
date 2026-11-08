import { StatusBanner } from './components/StatusBanner'

export default function App() {
  return (
    <main className="min-h-screen bg-slate-950 text-slate-100">
      <div className="mx-auto flex max-w-4xl flex-col gap-6 px-6 py-16">
        <div className="flex items-center gap-3">
          <span className="w-fit rounded-full bg-brand-500/15 px-3 py-1 text-sm font-medium text-brand-300">
            Sprint 1
          </span>
          <StatusBanner />
        </div>

        <div className="flex flex-col gap-4">
          <h1 className="text-4xl font-bold tracking-tight text-white">EventHub</h1>
          <p className="max-w-prose text-lg text-slate-400">
            Descubra eventos, compre ingressos e apresente seu QR Code na entrada.
          </p>
        </div>

        <ol className="grid gap-3 text-slate-300 sm:grid-cols-2">
          <li className="rounded-lg border border-slate-800 bg-slate-900/60 p-4">Spring Boot + PostgreSQL de pé</li>
          <li className="rounded-lg border border-slate-800 bg-slate-900/60 p-4">Migrações versionadas com Flyway</li>
          <li className="rounded-lg border border-slate-800 bg-slate-900/60 p-4">React + TypeScript + Tailwind</li>
          <li className="rounded-lg border border-slate-800 bg-slate-900/60 p-4">Health check consumido pelo frontend</li>
        </ol>
      </div>
    </main>
  )
}
