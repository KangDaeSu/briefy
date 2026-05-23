import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { authApi } from '../api/auth'
import './AuthPage.css'

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const token = searchParams.get('token') ?? ''

  const [form, setForm] = useState({ newPassword: '', confirm: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  if (!token) {
    return (
      <div className="auth-page">
        <div className="auth-card">
          <h1 className="auth-title">링크 오류</h1>
          <p className="auth-sub">유효하지 않은 비밀번호 재설정 링크입니다.</p>
          <p className="auth-link">
            <Link to="/forgot-password">비밀번호 찾기 다시 시도</Link>
          </p>
        </div>
      </div>
    )
  }

  function handleChange(e) {
    setForm(f => ({ ...f, [e.target.name]: e.target.value }))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')

    if (form.newPassword !== form.confirm) {
      setError('비밀번호가 일치하지 않습니다.')
      return
    }
    if (form.newPassword.length < 8) {
      setError('비밀번호는 8자 이상이어야 합니다.')
      return
    }

    setLoading(true)
    try {
      await authApi.resetPassword(token, form.newPassword)
      navigate('/login', { state: { message: '비밀번호가 변경됐습니다. 새 비밀번호로 로그인하세요.' } })
    } catch (err) {
      setError(err.body?.message ?? '링크가 만료됐거나 유효하지 않습니다. 비밀번호 찾기를 다시 시도해 주세요.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <h1 className="auth-title">새 비밀번호 설정</h1>
        <p className="auth-sub">8자 이상의 새 비밀번호를 입력하세요.</p>

        <form onSubmit={handleSubmit} className="auth-form">
          <label className="auth-label">
            새 비밀번호
            <input
              className="auth-input"
              type="password"
              name="newPassword"
              value={form.newPassword}
              onChange={handleChange}
              placeholder="••••••••"
              required
              autoFocus
            />
          </label>

          <label className="auth-label">
            비밀번호 확인
            <input
              className="auth-input"
              type="password"
              name="confirm"
              value={form.confirm}
              onChange={handleChange}
              placeholder="••••••••"
              required
            />
          </label>

          {error && <p className="auth-error">{error}</p>}

          <button className="auth-btn" type="submit" disabled={loading}>
            {loading ? '저장 중…' : '비밀번호 변경'}
          </button>
        </form>

        <p className="auth-link">
          <Link to="/login">← 로그인으로 돌아가기</Link>
        </p>
      </div>
    </div>
  )
}
