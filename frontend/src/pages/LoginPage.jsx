import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import './AuthPage.css'

const GOOGLE_LOGIN_URL = `${import.meta.env.VITE_API_BASE_URL ?? ''}/oauth2/authorization/google`

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  function handleChange(e) {
    setForm(f => ({ ...f, [e.target.name]: e.target.value }))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await login(form.email, form.password)
      navigate('/')
    } catch (err) {
      setError(err.body?.message ?? '로그인에 실패했습니다')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <h1 className="auth-title">briefy</h1>
        <p className="auth-sub">일정 관리 서비스</p>

        <form onSubmit={handleSubmit} className="auth-form">
          <label className="auth-label">
            이메일
            <input
              className="auth-input"
              type="email"
              name="email"
              value={form.email}
              onChange={handleChange}
              placeholder="you@example.com"
              required
            />
          </label>
          <label className="auth-label">
            비밀번호
            <input
              className="auth-input"
              type="password"
              name="password"
              value={form.password}
              onChange={handleChange}
              placeholder="••••••••"
              required
            />
          </label>
          {error && <p className="auth-error">{error}</p>}
          <button className="auth-btn" type="submit" disabled={loading}>
            {loading ? '로그인 중…' : '로그인'}
          </button>
        </form>

        <div className="auth-divider"><span>또는</span></div>

        <a className="auth-btn auth-btn--google" href={GOOGLE_LOGIN_URL}>
          Google로 로그인
        </a>

        <p className="auth-link">
          계정이 없으신가요? <Link to="/register">회원가입</Link>
        </p>
      </div>
    </div>
  )
}
