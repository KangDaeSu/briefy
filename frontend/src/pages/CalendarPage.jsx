import { useState, useEffect, useCallback } from 'react'
import MonthCalendar from '../components/MonthCalendar'
import ScheduleModal from '../components/ScheduleModal'
import { schedulesApi } from '../api/schedules'
import './CalendarPage.css'

const MONTH_NAMES = ['1월', '2월', '3월', '4월', '5월', '6월',
  '7월', '8월', '9월', '10월', '11월', '12월']

const today = new Date()

export default function CalendarPage() {
  const [year, setYear] = useState(today.getFullYear())
  const [month, setMonth] = useState(today.getMonth())
  const [events, setEvents] = useState([])
  const [loading, setLoading] = useState(false)
  const [selectedDate, setSelectedDate] = useState(null)
  const [modal, setModal] = useState({ open: false, schedule: null })

  const fetchEvents = useCallback(async (y, m, signal) => {
    setLoading(true)
    try {
      // UTC 경계를 명시적으로 지정해 로컬 시간 변환으로 인한 날짜 이탈 방지
      const from = new Date(Date.UTC(y, m, 1)).toISOString()
      const to   = new Date(Date.UTC(y, m + 1, 1)).toISOString()
      const res = await schedulesApi.list(from, to, { signal })
      setEvents(res.data ?? [])
    } catch (e) {
      if (e.name !== 'AbortError') console.error('일정 조회 실패', e)
    } finally {
      setLoading(false)
    }
  }, [])

  /* eslint-disable react-hooks/set-state-in-effect */
  useEffect(() => {
    const controller = new AbortController()
    fetchEvents(year, month, controller.signal)
    return () => controller.abort()
  }, [year, month, fetchEvents])
  /* eslint-enable react-hooks/set-state-in-effect */

  function prevMonth() {
    if (month === 0) { setYear(y => y - 1); setMonth(11) }
    else setMonth(m => m - 1)
  }
  function nextMonth() {
    if (month === 11) { setYear(y => y + 1); setMonth(0) }
    else setMonth(m => m + 1)
  }

  const selectedEvents = selectedDate
    ? events.filter(ev => {
        const d = new Date(ev.startTime)
        return d.getFullYear() === selectedDate.getFullYear() &&
          d.getMonth() === selectedDate.getMonth() &&
          d.getDate() === selectedDate.getDate()
      })
    : []

  async function handleSave(data) {
    if (modal.schedule) {
      await schedulesApi.update(modal.schedule.id, data)
    } else {
      await schedulesApi.create(data)
    }
    await fetchEvents(year, month)
  }

  async function handleDelete() {
    if (modal.schedule) {
      await schedulesApi.delete(modal.schedule.id)
      await fetchEvents(year, month)
    }
  }

  function openCreate() {
    setModal({ open: true, schedule: null })
  }

  function openEdit(ev) {
    setModal({ open: true, schedule: ev })
  }

  function fmt(iso) {
    return new Date(iso).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
  }

  return (
    <div className="cal-page">
      <header className="cal-page__header">
        <div className="cal-page__nav">
          <button className="nav-btn" onClick={prevMonth}>‹</button>
          <h1>{year}년 {MONTH_NAMES[month]}</h1>
          <button className="nav-btn" onClick={nextMonth}>›</button>
        </div>
        <button className="btn-add" onClick={openCreate}>+ 일정 추가</button>
      </header>

      {loading && <div className="cal-loading">로딩 중…</div>}

      <MonthCalendar
        year={year}
        month={month}
        events={events}
        selectedDate={selectedDate}
        onSelectDate={setSelectedDate}
      />

      {selectedDate && (
        <section className="day-panel">
          <h2 className="day-panel__title">
            {selectedDate.getMonth() + 1}월 {selectedDate.getDate()}일
            <span className="day-panel__count">{selectedEvents.length}건</span>
          </h2>
          {selectedEvents.length === 0 ? (
            <p className="day-panel__empty">일정이 없습니다. <button className="link-btn" onClick={openCreate}>추가하기</button></p>
          ) : (
            <ul className="day-panel__list">
              {selectedEvents.map((ev) => (
                <li key={ev.id} className="day-event" onClick={() => openEdit(ev)}>
                  <div className={`day-event__bar ${ev.recurring ? 'day-event__bar--recurring' : ''}`} />
                  <div className="day-event__body">
                    <span className="day-event__title">{ev.title}</span>
                    <span className="day-event__time">{fmt(ev.startTime)} – {fmt(ev.endTime)}</span>
                    {ev.recurring && <span className="day-event__tag">반복</span>}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>
      )}

      <ScheduleModal
        open={modal.open}
        onClose={() => setModal({ open: false, schedule: null })}
        onSave={handleSave}
        onDelete={handleDelete}
        defaultDate={selectedDate}
        schedule={modal.schedule}
      />
    </div>
  )
}
