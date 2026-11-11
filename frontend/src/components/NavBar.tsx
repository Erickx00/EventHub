import { Link, NavLink } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

const linkClass = 'text-slate-300 transition hover:text-white'

export function NavBar() {
  const { user, logout } = useAuth()

  return (
    <header className="border-b border-slate-800 bg-slate-950/80">
      <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-6 py-4">
        <Link to="/" className="text-lg font-semibold tracking-tight text-white">
          EventHub
        </Link>

        <nav className="flex items-center gap-4 text-sm">
          <NavLink to="/" className={linkClass} end>
            Eventos
          </NavLink>

          {user ? (
            <>
              <NavLink to="/tickets" className={linkClass}>
                Meus ingressos
              </NavLink>
              {user.role === 'ORGANIZER' || user.role === 'ADMIN' ? (
                <>
                  <NavLink to="/organizer" className={linkClass}>
                    Painel
                  </NavLink>
                  <NavLink to="/checkin" className={linkClass}>
                    Portaria
                  </NavLink>
                </>
              ) : null}
              <span className="hidden text-slate-500 sm:inline">{user.name}</span>
              <button
                type="button"
                onClick={() => void logout()}
                className="rounded-lg border border-slate-700 px-3 py-1.5 text-slate-200 transition hover:border-slate-500"
              >
                Sair
              </button>
            </>
          ) : (
            <Link
              to="/login"
              className="rounded-lg bg-brand-600 px-3 py-1.5 font-medium text-white transition hover:bg-brand-500"
            >
              Entrar
            </Link>
          )}
        </nav>
      </div>
    </header>
  )
}
