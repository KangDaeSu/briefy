import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { getSavedAccounts, removeAccount } from '../utils/savedAccounts'
import './AuthPage.css'

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()

  const [accounts, setAccounts] = useState(getSavedAccounts)
  const [selectedEmail, setSelectedEmail] = useState(null)
  const [showForm, setShowForm] = useState(accounts.length === 0)
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const selectedAccount = accounts.find(a => a.email === selectedEmail) ?? null

  function handleChange(e) {
    setForm(f => ({ ...f, [e.target.name]: e.target.value }))
  }

  function selectAccount(account) {
    setSelectedEmail(account.email)
    setForm({ email: account.email, password: '' })
    setShowForm(true)
    setError('')
  }

  function handleAddAccount() {
    setSelectedEmail(null)
    setForm({ email: '', password: '' })
    setShowForm(true)
    setError('')
  }

  function handleBackToList() {
    setShowForm(false)
    setSelectedEmail(null)
    setForm({ email: '', password: '' })
    setError('')
  }

  function handleRemove(e, email) {
    e.stopPropagation()
    removeAccount(email)
    const updated = getSavedAccounts()
    setAccounts(updated)
    if (updated.length === 0) setShowForm(true)
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

        {!showForm ? (
          <div className="account-list">
            {accounts.map(account => (
              <button
                key={account.email}
                type="button"
                className="account-item"
                onClick={() => selectAccount(account)}
              >
                <div className="account-avatar">
                  {(account.name || account.email)[0].toUpperCase()}
                </div>
                <div className="account-info">
                  <span className="account-name">{account.name}</span>
                  <span className="account-email">{account.email}</span>
                </div>
                <span
                  className="account-remove"
                  role="button"
                  aria-label="목록에서 제거"
                  onClick={(e) => handleRemove(e, account.email)}
                >
                  ✕
                </span>
              </button>
            ))}
            <button type="button" className="auth-btn auth-btn--ghost" onClick={handleAddAccount}>
              + 다른 계정으로 로그인
            </button>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="auth-form">
            {selectedAccount ? (
              <div className="selected-account">
                <div className="account-avatar account-avatar--lg">
                  {(selectedAccount.name || selectedAccount.email)[0].toUpperCase()}
                </div>
                <div className="account-info">
                  <span className="account-name">{selectedAccount.name}</span>
                  <span className="account-email">{selectedAccount.email}</span>
                </div>
              </div>
            ) : (
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
                  autoFocus
                />
              </label>
            )}

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
                autoFocus={!!selectedAccount}
              />
            </label>

            {error && <p className="auth-error">{error}</p>}

            <button className="auth-btn" type="submit" disabled={loading}>
              {loading ? '로그인 중…' : '로그인'}
            </button>

            {accounts.length > 0 && (
              <button type="button" className="auth-btn auth-btn--ghost" onClick={handleBackToList}>
                ← 계정 선택으로 돌아가기
              </button>
            )}
          </form>
        )}

        {/* <p className="auth-link">
          <Link to="/forgot-password">비밀번호를 잊으셨나요?</Link>
        </p> */}
        <p className="auth-link">
          계정이 없으신가요? <Link to="/register">회원가입</Link>
        </p>
      </div>
    </div>
  )
}
