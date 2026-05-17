import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import './SettingsPage.css'

export default function SettingsPage() {
  const { user, updateUser, deleteAccount } = useAuth()
  const navigate = useNavigate()

  const [name, setName] = useState(user?.name ?? '')
  const [nameStatus, setNameStatus] = useState(null) // null | 'saving' | 'saved' | 'error'
  const [nameError, setNameError] = useState('')

  const [deletePhase, setDeletePhase] = useState('idle') // 'idle' | 'confirm' | 'deleting'
  const [deleteError, setDeleteError] = useState('')

  async function handleNameSave(e) {
    e.preventDefault()
    if (!name.trim()) return
    setNameStatus('saving')
    setNameError('')
    try {
      await updateUser(name.trim())
      setNameStatus('saved')
      setTimeout(() => setNameStatus(null), 2000)
    } catch (err) {
      setNameError(err.body?.message ?? '저장에 실패했습니다')
      setNameStatus('error')
    }
  }

  async function handleDelete() {
    setDeletePhase('deleting')
    setDeleteError('')
    try {
      await deleteAccount()
      navigate('/login', { replace: true })
    } catch (err) {
      setDeleteError(err.body?.message ?? '계정 삭제에 실패했습니다')
      setDeletePhase('confirm')
    }
  }

  return (
    <div className="settings-page">
      <div className="settings-container">
        <h1 className="settings-title">프로필 설정</h1>

        {/* 기본 정보 */}
        <section className="settings-section">
          <h2 className="settings-section__heading">기본 정보</h2>

          <div className="settings-avatar">
            {user?.name?.charAt(0).toUpperCase()}
          </div>

          <div className="settings-field">
            <span className="settings-field__label">이메일</span>
            <span className="settings-field__value">{user?.email}</span>
          </div>

          <form onSubmit={handleNameSave} className="settings-name-form">
            <label className="settings-field__label" htmlFor="name-input">이름</label>
            <div className="settings-name-row">
              <input
                id="name-input"
                className="settings-input"
                type="text"
                value={name}
                onChange={e => setName(e.target.value)}
                maxLength={100}
                required
              />
              <button
                className="settings-btn settings-btn--primary"
                type="submit"
                disabled={nameStatus === 'saving' || name.trim() === user?.name}
              >
                {nameStatus === 'saving' ? '저장 중…' : nameStatus === 'saved' ? '저장됨' : '저장'}
              </button>
            </div>
            {nameStatus === 'error' && <p className="settings-error">{nameError}</p>}
          </form>
        </section>

        {/* 계정 관리 */}
        <section className="settings-section settings-section--danger">
          <h2 className="settings-section__heading">계정 관리</h2>
          <p className="settings-danger__desc">
            계정을 삭제하면 모든 일정 데이터가 영구적으로 삭제됩니다. 이 작업은 되돌릴 수 없습니다.
          </p>

          {deletePhase === 'idle' && (
            <button
              className="settings-btn settings-btn--danger"
              onClick={() => setDeletePhase('confirm')}
            >
              계정 삭제
            </button>
          )}

          {(deletePhase === 'confirm' || deletePhase === 'deleting') && (
            <div className="settings-confirm-box">
              <p className="settings-confirm__text">정말로 계정을 삭제하시겠습니까?</p>
              {deleteError && <p className="settings-error">{deleteError}</p>}
              <div className="settings-confirm__actions">
                <button
                  className="settings-btn settings-btn--ghost"
                  onClick={() => { setDeletePhase('idle'); setDeleteError('') }}
                  disabled={deletePhase === 'deleting'}
                >
                  취소
                </button>
                <button
                  className="settings-btn settings-btn--danger"
                  onClick={handleDelete}
                  disabled={deletePhase === 'deleting'}
                >
                  {deletePhase === 'deleting' ? '삭제 중…' : '삭제 확인'}
                </button>
              </div>
            </div>
          )}
        </section>
      </div>
    </div>
  )
}
