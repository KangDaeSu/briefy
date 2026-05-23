import { BrowserRouter, Link, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider, useAuth } from './contexts/AuthContext'
import { useDarkMode } from './hooks/useDarkMode'
import CalendarPage from './pages/CalendarPage'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import SettingsPage from './pages/SettingsPage'
// import ForgotPasswordPage from './pages/ForgotPasswordPage'
// import ResetPasswordPage from './pages/ResetPasswordPage'
import ProtectedRoute from './components/ProtectedRoute'
import './App.css'

function AppNav() {
  const { user, logout } = useAuth()
  const [dark, toggleDark] = useDarkMode()
  return (
    <nav className="app-nav">
      <Link to="/" className="app-logo">briefy</Link>
      <div className="app-nav__user">
        <button
          className="app-nav__theme-btn"
          onClick={toggleDark}
          aria-label={dark ? '라이트 모드로 전환' : '다크 모드로 전환'}
        >
          {dark ? '☀' : '🌙'}
        </button>
        {user && (
          <>
            <Link to="/settings" className="app-nav__name">{user.name}</Link>
            <button className="app-nav__logout" onClick={logout}>로그아웃</button>
          </>
        )}
      </div>
    </nav>
  )
}

function AppShell() {
  return (
    <div className="app">
      <AppNav />
      <main>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          {/* <Route path="/forgot-password" element={<ForgotPasswordPage />} /> */}
          {/* <Route path="/reset-password" element={<ResetPasswordPage />} /> */}
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <CalendarPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/settings"
            element={
              <ProtectedRoute>
                <SettingsPage />
              </ProtectedRoute>
            }
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppShell />
      </AuthProvider>
    </BrowserRouter>
  )
}
