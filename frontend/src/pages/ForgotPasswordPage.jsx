import { useState } from 'react'
import { Link } from 'react-router-dom'
import { authApi } from '../api/auth'
import './AuthPage.css'

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await authApi.forgotPassword(email)
      setSubmitted(true)
    } catch {
      setError('요청 처리 중 문제가 발생했습니다. 잠시 후 다시 시도해 주세요.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <h1 className="auth-title">비밀번호 찾기</h1>

        {submitted ? (
          <>
            <p className="auth-sub" style={{ marginBottom: '1.5rem' }}>
              입력하신 이메일로 재설정 링크를 보냈습니다.<br />
              메일함을 확인해 주세요 (유효 시간: 30분).
            </p>
            <p className="auth-link">
              <Link to="/login">로그인으로 돌아가기</Link>
            </p>
          </>
        ) : (
          <>
            <p className="auth-sub">
              가입할 때 사용한 이메일을 입력하세요.
            </p>
            <form onSubmit={handleSubmit} className="auth-form">
              <label className="auth-label">
                이메일
                <input
                  className="auth-input"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="you@example.com"
                  required
                  autoFocus
                />
              </label>

              {error && <p className="auth-error">{error}</p>}

              <button className="auth-btn" type="submit" disabled={loading}>
                {loading ? '전송 중…' : '재설정 링크 보내기'}
              </button>
            </form>

            <p className="auth-link">
              <Link to="/login">← 로그인으로 돌아가기</Link>
            </p>
          </>
        )}
      </div>
    </div>
  )
}
