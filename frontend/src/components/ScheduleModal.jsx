import { useState, useEffect, useRef } from 'react'
import './ScheduleModal.css'

const RRULE_OPTIONS = [
  { value: '', label: '반복 없음' },
  { value: 'FREQ=DAILY', label: '매일' },
  { value: 'FREQ=WEEKLY', label: '매주 같은 요일' },
  { value: 'FREQ=WEEKLY;INTERVAL=2', label: '격주 같은 요일' },
  { value: 'FREQ=MONTHLY', label: '매월 같은 날' },
  { value: 'FREQ=YEARLY', label: '매년 같은 날' },
  { value: 'FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR', label: '주중 매일 (월–금)' },
  { value: '__custom__', label: '직접 입력…' },
]

const PRESET_VALUES = RRULE_OPTIONS.map(o => o.value).filter(v => v !== '__custom__')

function to12h(h24) {
  if (h24 === 0) return { hour: '0', ampm: '오전' }
  if (h24 < 12) return { hour: String(h24), ampm: '오전' }
  if (h24 === 12) return { hour: '12', ampm: '오후' }
  return { hour: String(h24 - 12), ampm: '오후' }
}

function to24h(hour12, ampm) {
  const h = parseInt(hour12, 10)
  if (isNaN(h)) return 0
  if (h >= 13 && h <= 23) return h
  if (h === 0 || h === 24) return 0
  if (ampm === '오전') return h === 12 ? 0 : h
  return h === 12 ? 12 : h + 12
}

function parseToFields(iso) {
  if (!iso) return { date: '', hour: '12', minute: '00', ampm: '오전' }
  const d = new Date(iso)
  const pad = n => String(n).padStart(2, '0')
  const date = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  const { hour, ampm } = to12h(d.getHours())
  const minute = pad(d.getMinutes())
  return { date, hour, minute, ampm }
}

function fieldsToISO(date, hour, minute, ampm) {
  if (!date) return ''
  const h24 = to24h(hour, ampm)
  const m = Math.max(0, Math.min(59, parseInt(minute, 10) || 0))
  const pad = n => String(n).padStart(2, '0')
  return new Date(`${date}T${pad(h24)}:${pad(m)}`).toISOString()
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
  skipHolidays: false,
}

function CalendarIcon() {
  return (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
      <line x1="16" y1="2" x2="16" y2="6" />
      <line x1="8" y1="2" x2="8" y2="6" />
      <line x1="3" y1="10" x2="21" y2="10" />
    </svg>
  )
}

export default function ScheduleModal({ open, onClose, onSave, onDelete, defaultDate, schedule }) {
  const [form, setForm] = useState(INITIAL)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState(null)
  const startDtRef = useRef(null)
  const endDtRef = useRef(null)

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
        skipHolidays: schedule.skipHolidays ?? false,
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

  // datetime-local 값으로 모든 시간 필드를 한번에 업데이트
  function handleDtLocal(prefix) {
    return e => {
      const val = e.target.value
      if (!val) return
      const [datePart, timePart] = val.split('T')
      const [h24Str, minStr] = timePart.split(':')
      const { hour, ampm } = to12h(parseInt(h24Str, 10))
      setForm(prev => ({
        ...prev,
        [`${prefix}Date`]: datePart,
        [`${prefix}Hour`]: hour,
        [`${prefix}Minute`]: minStr,
        [`${prefix}AmPm`]: ampm,
      }))
    }
  }

  // 현재 폼 상태를 datetime-local value 형식으로 변환
  function dtLocalValue(prefix) {
    const date = form[`${prefix}Date`]
    if (!date) return ''
    const h24 = to24h(form[`${prefix}Hour`] || '0', form[`${prefix}AmPm`])
    const m = parseInt(form[`${prefix}Minute`] || '0', 10)
    const pad = n => String(n).padStart(2, '0')
    return `${date}T${pad(h24)}:${pad(m)}`
  }

  function openPicker(ref) {
    try { ref.current?.showPicker() } catch { ref.current?.focus() }
  }

  function handleHourChange(prefix) {
    return e => {
      const raw = e.target.value.replace(/\D/g, '')
      if (raw === '') { setForm(prev => ({ ...prev, [`${prefix}Hour`]: '' })); return }
      const n = parseInt(raw, 10)
      setForm(prev => ({ ...prev, [`${prefix}Hour`]: String(Math.min(24, n)) }))
    }
  }

  function handleMinuteChange(prefix) {
    return e => {
      const raw = e.target.value.replace(/\D/g, '')
      if (raw === '') { setForm(prev => ({ ...prev, [`${prefix}Minute`]: '' })); return }
      const n = parseInt(raw, 10)
      setForm(prev => ({ ...prev, [`${prefix}Minute`]: String(Math.min(59, n)) }))
    }
  }

  function handleHourBlur(prefix) {
    return () => {
      const h = parseInt(form[`${prefix}Hour`], 10)
      if (isNaN(h)) { setForm(prev => ({ ...prev, [`${prefix}Hour`]: '0' })); return }
      if (h >= 13 && h <= 23) {
        setForm(prev => ({ ...prev, [`${prefix}Hour`]: String(h - 12), [`${prefix}AmPm`]: '오후' }))
      } else if (h === 24) {
        const dateKey = `${prefix}Date`
        setForm(prev => {
          const currentDate = prev[dateKey]
          if (!currentDate) return { ...prev, [`${prefix}Hour`]: '0', [`${prefix}AmPm`]: '오전' }
          const d = new Date(currentDate + 'T00:00:00')
          d.setDate(d.getDate() + 1)
          const pad = n => String(n).padStart(2, '0')
          const newDate = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
          return { ...prev, [`${prefix}Hour`]: '0', [`${prefix}AmPm`]: '오전', [dateKey]: newDate }
        })
      } else if (h === 0) {
        setForm(prev => ({ ...prev, [`${prefix}Hour`]: '0', [`${prefix}AmPm`]: '오전' }))
      }
    }
  }

  function handleMinuteBlur(prefix) {
    return () => {
      const m = parseInt(form[`${prefix}Minute`], 10)
      const clamped = isNaN(m) ? 0 : m
      setForm(prev => ({ ...prev, [`${prefix}Minute`]: String(clamped).padStart(2, '0') }))
    }
  }

  function handleRrulePreset(e) {
    const val = e.target.value
    if (val !== '__custom__') setForm(prev => ({ ...prev, rrule: val }))
  }

  const isCustomRrule = form.rrule !== '' && !PRESET_VALUES.includes(form.rrule)
  const rruleSelectValue = isCustomRrule ? '__custom__' : form.rrule

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
        skipHolidays: form.skipHolidays,
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

          <div className="datetime-fields">
            {[
              { prefix: 'start', label: '시작', ref: startDtRef },
              { prefix: 'end',   label: '종료', ref: endDtRef },
            ].map(({ prefix, label, ref }) => (
              <div key={prefix} className="datetime-field">
                <span className="field-label">{label} <span className="required">*</span></span>
                <div className="datetime-box">
                  <input
                    type="date"
                    className="date-input"
                    value={form[`${prefix}Date`]}
                    onChange={set(`${prefix}Date`)}
                    required
                  />
                  <input
                    className="time-hour-input"
                    type="text"
                    inputMode="numeric"
                    value={form[`${prefix}Hour`]}
                    onChange={handleHourChange(prefix)}
                    onBlur={handleHourBlur(prefix)}
                    placeholder="시"
                  />
                  <span className="time-colon">:</span>
                  <input
                    className="time-minute-input"
                    type="text"
                    inputMode="numeric"
                    value={form[`${prefix}Minute`]}
                    onChange={handleMinuteChange(prefix)}
                    onBlur={handleMinuteBlur(prefix)}
                    placeholder="분"
                  />
                  <select className="ampm-select" value={form[`${prefix}AmPm`]} onChange={set(`${prefix}AmPm`)}>
                    <option value="오전">오전</option>
                    <option value="오후">오후</option>
                  </select>
                  <div className="calendar-btn-wrap">
                    <input
                      ref={ref}
                      type="datetime-local"
                      className="hidden-dt-input"
                      value={dtLocalValue(prefix)}
                      onChange={handleDtLocal(prefix)}
                      tabIndex={-1}
                    />
                    <button type="button" className="calendar-btn" onClick={() => openPicker(ref)}>
                      <CalendarIcon />
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>

          <label>
            반복
            <select value={rruleSelectValue} onChange={handleRrulePreset}>
              {RRULE_OPTIONS.map(o => (
                <option key={o.value} value={o.value}>{o.label}</option>
              ))}
            </select>
            {isCustomRrule && (
              <input
                value={form.rrule}
                onChange={set('rrule')}
                placeholder="예: FREQ=WEEKLY;BYDAY=MO,WE,FR"
              />
            )}
            {form.rrule && (
              <label className="skip-holidays-opt">
                <input
                  type="checkbox"
                  checked={form.skipHolidays}
                  onChange={e => setForm(prev => ({ ...prev, skipHolidays: e.target.checked }))}
                />
                공휴일·대체휴일 건너뛰기
              </label>
            )}
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
