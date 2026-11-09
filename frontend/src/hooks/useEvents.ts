import { useEffect, useState } from 'react'
import { api, type EventQuery, type EventSummary, type Page } from '../lib/api'

export type EventsState =
  | { readonly kind: 'loading' }
  | { readonly kind: 'ok'; readonly data: Page<EventSummary> }
  | { readonly kind: 'error'; readonly message: string }

/**
 * Busca eventos publicados. A chave serializada evita refetch quando o objeto
 * de query muda de identidade mas nao de conteudo.
 */
export function useEvents(query: EventQuery): EventsState {
  const [state, setState] = useState<EventsState>({ kind: 'loading' })
  const key = JSON.stringify(query)

  useEffect(() => {
    let active = true
    setState({ kind: 'loading' })

    api.events
      .list(query)
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key])

  return state
}
