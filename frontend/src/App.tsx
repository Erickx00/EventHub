import type { ReactElement } from 'react'
import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { NavBar } from './components/NavBar'
import { useAuth } from './contexts/AuthContext'
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
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <NavBar />

      <main className="mx-auto max-w-6xl px-6 py-10">
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
            path="*"
            element={<p className="text-sm text-slate-400">Pagina nao encontrada.</p>}
          />
        </Routes>
      </main>
    </div>
  )
}
