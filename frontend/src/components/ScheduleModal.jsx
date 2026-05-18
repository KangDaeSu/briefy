import { useState, useEffect } from 'react'
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
  if (h >= 13 && h <= 23) return h         // 24h 직접 입력
  if (h === 0 || h === 24) return 0         // 자정
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

  // 숫자만 허용하며 즉시 클램핑 (type="text"이므로 setState로 강제 동기화)
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

  // 블러 시 13-24 자동 변환 + AM/PM 업데이트
  function handleHourBlur(prefix) {
    return () => {
      const h = parseInt(form[`${prefix}Hour`], 10)
      if (isNaN(h)) { setForm(prev => ({ ...prev, [`${prefix}Hour`]: '0' })); return }
      if (h >= 13 && h <= 23) {
        setForm(prev => ({ ...prev, [`${prefix}Hour`]: String(h - 12), [`${prefix}AmPm`]: '오후' }))
      } else if (h === 0 || h === 24) {
        setForm(prev => ({ ...prev, [`${prefix}Hour`]: '0', [`${prefix}AmPm`]: '오전' }))
      }
    }
  }

  // 블러 시 두 자리 패딩
  function handleMinuteBlur(prefix) {
    return () => {
      const m = parseInt(form[`${prefix}Minute`], 10)
      const clamped = isNaN(m) ? 0 : m
      setForm(prev => ({ ...prev, [`${prefix}Minute`]: String(clamped).padStart(2, '0') }))
    }
  }

  // 반복 셀렉트: 프리셋 선택 시 rrule 직접 세팅, 직접입력 선택 시 유지
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
                <input
                  className="time-hour-input"
                  type="text"
                  inputMode="numeric"
                  value={form.startHour}
                  onChange={handleHourChange('start')}
                  onBlur={handleHourBlur('start')}
                  placeholder="시"
                />
                <span className="time-colon">:</span>
                <input
                  className="time-minute-input"
                  type="text"
                  inputMode="numeric"
                  value={form.startMinute}
                  onChange={handleMinuteChange('start')}
                  onBlur={handleMinuteBlur('start')}
                  placeholder="분"
                />
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
                <input
                  className="time-hour-input"
                  type="text"
                  inputMode="numeric"
                  value={form.endHour}
                  onChange={handleHourChange('end')}
                  onBlur={handleHourBlur('end')}
                  placeholder="시"
                />
                <span className="time-colon">:</span>
                <input
                  className="time-minute-input"
                  type="text"
                  inputMode="numeric"
                  value={form.endMinute}
                  onChange={handleMinuteChange('end')}
                  onBlur={handleMinuteBlur('end')}
                  placeholder="분"
                />
                <select value={form.endAmPm} onChange={set('endAmPm')}>
                  <option value="오전">오전</option>
                  <option value="오후">오후</option>
                </select>
              </div>
            </label>
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
