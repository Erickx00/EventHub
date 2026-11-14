import { Link, NavLink } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

const linkClass = 'btn-ghost'

export function NavBar() {
  const { user, logout } = useAuth()

  return (
    <header className="sticky top-0 z-40 border-b border-brand-700/10 bg-white/85 backdrop-blur">
      <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-5 py-3">
        <Link to="/" className="flex items-center gap-2 text-lg font-extrabold tracking-tight text-ink">
          <span className="grid h-8 w-8 place-items-center rounded-xl bg-gradient-to-br from-brand-500 to-brand-700 text-sm font-black text-white">
            E
          </span>
          EventHub
        </Link>

        <nav className="flex items-center gap-1 sm:gap-2">
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
              <span className="hidden max-w-32 truncate text-sm text-ink-soft md:inline">{user.name}</span>
              <button type="button" onClick={() => void logout()} className="btn-secondary">
                Sair
              </button>
            </>
          ) : (
            <Link to="/login" className="btn-primary">
              Entrar
            </Link>
          )}
        </nav>
      </div>
    </header>
  )
}
