import { useState, useEffect } from 'react'
import './ScheduleModal.css'

const HOURS = Array.from({ length: 12 }, (_, i) => String(i + 1))
const MINUTES = ['00', '05', '10', '15', '20', '25', '30', '35', '40', '45', '50', '55']

function to12h(h24) {
  if (h24 === 0) return { hour: '12', ampm: '오전' }
  if (h24 < 12) return { hour: String(h24), ampm: '오전' }
  if (h24 === 12) return { hour: '12', ampm: '오후' }
  return { hour: String(h24 - 12), ampm: '오후' }
}

function to24h(hour12, ampm) {
  const h = parseInt(hour12, 10)
  if (ampm === '오전') return h === 12 ? 0 : h
  return h === 12 ? 12 : h + 12
}

function parseToFields(iso) {
  if (!iso) return { date: '', hour: '12', minute: '00', ampm: '오전' }
  const d = new Date(iso)
  const pad = n => String(n).padStart(2, '0')
  const date = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  const { hour, ampm } = to12h(d.getHours())
  const rawMin = d.getMinutes()
  const minute = pad(Math.round(rawMin / 5) * 5 % 60)
  return { date, hour, minute, ampm }
}

function fieldsToISO(date, hour, minute, ampm) {
  if (!date) return ''
  const h24 = to24h(hour, ampm)
  return new Date(`${date}T${String(h24).padStart(2, '0')}:${minute}`).toISOString()
}

const INITIAL = {
  title: '',
  description: '',
  startDate: '',
  startHour: '9',
  startMinute: '00',
  startAmPm: '오전',
  endDate: '',
  endHour: '10',
  endMinute: '00',
  endAmPm: '오전',
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
      const s = parseToFields(schedule.startTime)
      const e = parseToFields(schedule.endTime)
      setForm({
        title: schedule.title,
        description: schedule.description ?? '',
        startDate: s.date,
        startHour: s.hour,
        startMinute: s.minute,
        startAmPm: s.ampm,
        endDate: e.date,
        endHour: e.hour,
        endMinute: e.minute,
        endAmPm: e.ampm,
        rrule: schedule.rrule ?? '',
      })
    } else if (defaultDate) {
      const pad = n => String(n).padStart(2, '0')
      const d = defaultDate
      const date = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
      setForm(prev => ({
        ...prev,
        startDate: date,
        startHour: '9',
        startMinute: '00',
        startAmPm: '오전',
        endDate: date,
        endHour: '10',
        endMinute: '00',
        endAmPm: '오전',
      }))
    }
  }, [open, schedule, defaultDate])
  /* eslint-enable react-hooks/set-state-in-effect */

  if (!open) return null

  const set = key => e => setForm(prev => ({ ...prev, [key]: e.target.value }))

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await onSave({
        title: form.title,
        description: form.description || null,
        startTime: fieldsToISO(form.startDate, form.startHour, form.startMinute, form.startAmPm),
        endTime: fieldsToISO(form.endDate, form.endHour, form.endMinute, form.endAmPm),
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
    <div className="modal-overlay" onClick={e => e.target === e.currentTarget && onClose()}>
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
              <input type="date" value={form.startDate} onChange={set('startDate')} required />
              <div className="time-select-wrap">
                <select value={form.startHour} onChange={set('startHour')}>
                  {HOURS.map(h => <option key={h} value={h}>{h}</option>)}
                </select>
                <span className="time-colon">:</span>
                <select value={form.startMinute} onChange={set('startMinute')}>
                  {MINUTES.map(m => <option key={m} value={m}>{m}</option>)}
                </select>
                <select value={form.startAmPm} onChange={set('startAmPm')}>
                  <option value="오전">오전</option>
                  <option value="오후">오후</option>
                </select>
              </div>
            </label>
            <label>
              종료
              <input type="date" value={form.endDate} onChange={set('endDate')} required />
              <div className="time-select-wrap">
                <select value={form.endHour} onChange={set('endHour')}>
                  {HOURS.map(h => <option key={h} value={h}>{h}</option>)}
                </select>
                <span className="time-colon">:</span>
                <select value={form.endMinute} onChange={set('endMinute')}>
                  {MINUTES.map(m => <option key={m} value={m}>{m}</option>)}
                </select>
                <select value={form.endAmPm} onChange={set('endAmPm')}>
                  <option value="오전">오전</option>
                  <option value="오후">오후</option>
                </select>
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
