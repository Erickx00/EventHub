import { useHealth } from '../hooks/useHealth'

const base = 'inline-flex items-center gap-2 rounded-full px-3 py-1 text-sm font-medium'

export function StatusBanner() {
  const health = useHealth()

  if (health.kind === 'loading') {
    return <span className={`${base} bg-slate-800 text-slate-300`}>verificando API…</span>
  }

  if (health.kind === 'error') {
    return (
      <span className={`${base} bg-amber-500/15 text-amber-300`} role="status">
        API offline — suba o backend em :8080
      </span>
    )
  }

  return (
    <span className={`${base} bg-emerald-500/15 text-emerald-300`} role="status">
      API {health.data.status} · {health.data.service}
    </span>
  )
}
