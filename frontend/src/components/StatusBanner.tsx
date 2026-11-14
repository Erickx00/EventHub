import { useHealth } from '../hooks/useHealth'

export function StatusBanner() {
  const health = useHealth()

  if (health.kind === 'loading') {
    return <span className="chip chip-neutral">verificando API…</span>
  }

  if (health.kind === 'error') {
    return <span className="chip bg-amber-100 text-amber-800">API indisponível</span>
  }

  return <span className="chip chip-success">API {health.data.status} · {health.data.service}</span>
}
