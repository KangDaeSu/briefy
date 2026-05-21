import { useMemo } from 'react'
import { getHoliday } from '../utils/holidays'
import './MonthCalendar.css'

const DAYS = ['일', '월', '화', '수', '목', '금', '토']

function isSameDay(a, b) {
  return a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
}

function getMonthGrid(year, month) {
  const firstDay = new Date(year, month, 1)
  const lastDay = new Date(year, month + 1, 0)
  const cells = []
  for (let i = 0; i < firstDay.getDay(); i++) cells.push(null)
  for (let d = 1; d <= lastDay.getDate(); d++) cells.push(new Date(year, month, d))
  return cells
}

export default function MonthCalendar({ year, month, events, selectedDate, onSelectDate }) {
  const today = new Date()
  const cells = useMemo(() => getMonthGrid(year, month), [year, month])

  const eventMap = useMemo(() => {
    const map = {}
    events.forEach(ev => {
      const d = new Date(ev.startTime)
      const key = `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`
      if (!map[key]) map[key] = []
      map[key].push(ev)
    })
    return map
  }, [events])

  return (
    <div className="month-calendar">
      <div className="cal-header">
        {DAYS.map((d, i) => (
          <div
            key={d}
            className={[
              'cal-day-name',
              i === 0 && 'cal-day-name--sun',
              i === 6 && 'cal-day-name--sat',
            ].filter(Boolean).join(' ')}
          >
            {d}
          </div>
        ))}
      </div>
      <div className="cal-grid">
        {cells.map((date, i) => {
          if (!date) return <div key={`empty-${i}`} className="cal-cell cal-cell--empty" />
          const key = `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`
          const dayEvents = eventMap[key] ?? []
          const isToday = isSameDay(date, today)
          const isSelected = selectedDate && isSameDay(date, selectedDate)
          const dow = date.getDay()
          const holiday = getHoliday(date)
          const isRed = dow === 0 || holiday !== null
          const isSat = dow === 6

          return (
            <div
              key={key}
              className={[
                'cal-cell',
                isToday && 'cal-cell--today',
                isSelected && 'cal-cell--selected',
                isRed && 'cal-cell--red-day',
                isSat && 'cal-cell--sat-day',
              ].filter(Boolean).join(' ')}
              onClick={() => onSelectDate(date)}
            >
              <div className="cal-cell__top">
                <span className={[
                  'cal-cell__date',
                  !isToday && isRed && 'cal-cell__date--red',
                  !isToday && isSat && 'cal-cell__date--blue',
                ].filter(Boolean).join(' ')}>
                  {date.getDate()}
                </span>
                {holiday && <span className="cal-cell__holiday">{holiday}</span>}
              </div>
              <div className="cal-cell__events">
                {dayEvents.slice(0, 3).map((ev, idx) => (
                  <div key={ev.id ?? idx} className={`cal-event-dot ${ev.recurring ? 'cal-event-dot--recurring' : ''}`}>
                    {ev.title}
                  </div>
                ))}
                {dayEvents.length > 3 && (
                  <div className="cal-event-more">+{dayEvents.length - 3}</div>
                )}
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
