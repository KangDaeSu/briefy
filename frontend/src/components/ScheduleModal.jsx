import { useState, useEffect } from 'react'
import './ScheduleModal.css'

function toLocalDatetimeValue(dateOrStr) {
  if (!dateOrStr) return ''
  const d = new Date(dateOrStr)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function getAmPm(datetimeStr) {
  if (!datetimeStr) return null
  const hour = parseInt(datetimeStr.split('T')[1]?.split(':')[0] ?? '0', 10)
  return hour < 12 ? '오전' : '오후'
}

const INITIAL = {
  title: '',
  description: '',
  startTime: '',
  endTime: '',
  rrule: '',
}

export default function ScheduleModal({ open, onClose, onSave, onDelete, defaultDate, schedule }) {
  const [form, setForm] = useState(INITIAL)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState(null)

  /* eslint-disable react-hooks/set-state-in-effect */
  useEffect(() => {
    if (!open) { setForm(INITIAL); setError(null); return }
    if (schedule) {
      setForm({
        title: schedule.title,
        description: schedule.description ?? '',
        startTime: toLocalDatetimeValue(schedule.startTime),
        endTime: toLocalDatetimeValue(schedule.endTime),
        rrule: schedule.rrule ?? '',
      })
    } else if (defaultDate) {
      const pad = (n) => String(n).padStart(2, '0')
      const d = defaultDate
      const base = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
      setForm(prev => ({
        ...prev,
        startTime: `${base}T09:00`,
        endTime: `${base}T10:00`,
      }))
    }
  }, [open, schedule, defaultDate])
  /* eslint-enable react-hooks/set-state-in-effect */

  if (!open) return null

  const set = (key) => (e) => setForm(prev => ({ ...prev, [key]: e.target.value }))

  function toggleAmPm(key) {
    setForm(prev => {
      const val = prev[key]
      if (!val) return prev
      const [date, time] = val.split('T')
      const [h, m] = time.split(':').map(Number)
      const newHour = h < 12 ? Math.min(h + 12, 23) : h - 12
      return { ...prev, [key]: `${date}T${String(newHour).padStart(2, '0')}:${String(m).padStart(2, '0')}` }
    })
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await onSave({
        title: form.title,
        description: form.description || null,
        startTime: new Date(form.startTime).toISOString(),
        endTime: new Date(form.endTime).toISOString(),
        rrule: form.rrule || null,
      })
      onClose()
    } catch (err) {
      setError(err.body?.message ?? err.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDelete() {
    if (!window.confirm('이 일정을 삭제하시겠습니까?')) return
    setSubmitting(true)
    try {
      await onDelete()
      onClose()
    } catch (err) {
      setError(err.body?.message ?? err.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="modal-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal-box">
        <div className="modal-header">
          <h2>{schedule ? '일정 수정' : '새 일정'}</h2>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>

        <form onSubmit={handleSubmit} className="modal-form">
          <label>
            제목 <span className="required">*</span>
            <input value={form.title} onChange={set('title')} required placeholder="일정 제목" />
          </label>
          <label>
            설명
            <textarea value={form.description} onChange={set('description')} rows={2} placeholder="선택 사항" />
          </label>
          <div className="modal-row">
            <label>
              시작
              <div className="time-input-wrap">
                <input type="datetime-local" value={form.startTime} onChange={set('startTime')} required />
                {form.startTime && (
                  <button
                    type="button"
                    className={`ampm-badge${getAmPm(form.startTime) === '오후' ? ' ampm-badge--pm' : ''}`}
                    onClick={() => toggleAmPm('startTime')}
                  >
                    {getAmPm(form.startTime)}
                  </button>
                )}
              </div>
            </label>
            <label>
              종료
              <div className="time-input-wrap">
                <input type="datetime-local" value={form.endTime} onChange={set('endTime')} required />
                {form.endTime && (
                  <button
                    type="button"
                    className={`ampm-badge${getAmPm(form.endTime) === '오후' ? ' ampm-badge--pm' : ''}`}
                    onClick={() => toggleAmPm('endTime')}
                  >
                    {getAmPm(form.endTime)}
                  </button>
                )}
              </div>
            </label>
          </div>
          <label>
            반복 규칙 <span className="hint">(RRULE, 선택)</span>
            <input
              value={form.rrule}
              onChange={set('rrule')}
              placeholder="예: FREQ=WEEKLY;BYDAY=MO,WE,FR"
            />
          </label>

          {error && <p className="modal-error">{error}</p>}

          <div className="modal-footer">
            {schedule && (
              <button type="button" className="btn btn--danger" onClick={handleDelete} disabled={submitting}>
                삭제
              </button>
            )}
            <div className="modal-footer-right">
              <button type="button" className="btn btn--ghost" onClick={onClose} disabled={submitting}>
                취소
              </button>
              <button type="submit" className="btn btn--primary" disabled={submitting}>
                {submitting ? '저장 중…' : schedule ? '수정' : '생성'}
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
  )
}
