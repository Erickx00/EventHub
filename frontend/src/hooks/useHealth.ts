import { useEffect, useState } from 'react'
import { api, type HealthResponse } from '../lib/api'

export type HealthState =
  | { readonly kind: 'loading' }
  | { readonly kind: 'ok'; readonly data: HealthResponse }
  | { readonly kind: 'error'; readonly message: string }

export function useHealth(): HealthState {
  const [state, setState] = useState<HealthState>({ kind: 'loading' })

  useEffect(() => {
    let active = true

    api
      .health()
      .then((data) => {
        if (active) setState({ kind: 'ok', data })
      })
      .catch((error: unknown) => {
        if (!active) return
        const message = error instanceof Error ? error.message : 'Erro desconhecido'
        setState({ kind: 'error', message })
      })

    return () => {
      active = false
    }
  }, [])

  return state
}
