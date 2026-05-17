import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import './AuthPage.css'

export default function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ email: '', name: '', password: '' })
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
      await register(form.email, form.name, form.password)
      navigate('/')
    } catch (err) {
      setError(err.body?.message ?? '회원가입에 실패했습니다')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <h1 className="auth-title">briefy</h1>
        <p className="auth-sub">회원가입</p>

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
            이름
            <input
              className="auth-input"
              type="text"
              name="name"
              value={form.name}
              onChange={handleChange}
              placeholder="홍길동"
              required
            />
          </label>
          <label className="auth-label">
            비밀번호 <span className="auth-hint">(8자 이상)</span>
            <input
              className="auth-input"
              type="password"
              name="password"
              value={form.password}
              onChange={handleChange}
              placeholder="••••••••"
              minLength={8}
              required
            />
          </label>
          {error && <p className="auth-error">{error}</p>}
          <button className="auth-btn" type="submit" disabled={loading}>
            {loading ? '처리 중…' : '회원가입'}
          </button>
        </form>

        <p className="auth-link">
          이미 계정이 있으신가요? <Link to="/login">로그인</Link>
        </p>
      </div>
    </div>
  )
}
