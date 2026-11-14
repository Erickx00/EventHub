import type { ReactElement } from 'react'
import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { NavBar } from './components/NavBar'
import { useAuth } from './contexts/AuthContext'
import { CheckInPage } from './pages/CheckInPage'
import { EventDetailsPage } from './pages/EventDetailsPage'
import { HomePage } from './pages/HomePage'
import { LoginPage } from './pages/LoginPage'
import { MyTicketsPage } from './pages/MyTicketsPage'
import { OrganizerPage } from './pages/OrganizerPage'

/**
 * Rota protegida no cliente e apenas conveniencia de UX. A autorizacao real e
 * sempre do backend; nenhuma regra de negocio depende deste guard.
 */
function RequireAuth({ children }: { children: ReactElement }) {
  const { user } = useAuth()
  const location = useLocation()

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  return children
}

export default function App() {
  return (
    <div className="flex min-h-screen flex-col bg-canvas text-ink">
      <NavBar />

      <main className="flex-1">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/events/:id" element={<EventDetailsPage />} />
          <Route
            path="/tickets"
            element={
              <RequireAuth>
                <MyTicketsPage />
              </RequireAuth>
            }
          />
          <Route
            path="/organizer"
            element={
              <RequireAuth>
                <OrganizerPage />
              </RequireAuth>
            }
          />
          <Route
            path="/checkin"
            element={
              <RequireAuth>
                <CheckInPage />
              </RequireAuth>
            }
          />
          <Route
            path="*"
            element={
              <div className="mx-auto max-w-3xl px-5 py-20 text-center">
                <p className="text-lg font-bold text-ink">Página não encontrada</p>
                <p className="mt-1 text-sm text-ink-soft">Volte para a lista de eventos.</p>
              </div>
            }
          />
        </Routes>
      </main>

      <footer className="border-t border-brand-700/10 bg-white">
        <div className="mx-auto flex max-w-6xl flex-col gap-1 px-5 py-6 text-xs text-ink-faint sm:flex-row sm:items-center sm:justify-between">
          <span>EventHub · projeto de portfólio</span>
          <span>Java 21 · Spring Boot 4 · React 19</span>
        </div>
      </footer>
    </div>
  )
}
