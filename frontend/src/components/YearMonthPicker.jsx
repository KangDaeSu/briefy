import { useState } from 'react'
import './YearMonthPicker.css'

const MONTHS = ['1월', '2월', '3월', '4월', '5월', '6월',
  '7월', '8월', '9월', '10월', '11월', '12월']

export default function YearMonthPicker({ year, month, onSelect }) {
  const [pickerYear, setPickerYear] = useState(year)
  const today = new Date()

  return (
    <div className="ym-picker">
      <div className="ym-picker__year-row">
        <button className="ym-picker__arrow" onClick={() => setPickerYear(y => y - 1)}>‹</button>
        <span className="ym-picker__year">{pickerYear}년</span>
        <button className="ym-picker__arrow" onClick={() => setPickerYear(y => y + 1)}>›</button>
      </div>
      <div className="ym-picker__months">
        {MONTHS.map((name, i) => {
          const isSelected = pickerYear === year && i === month
          const isCurrent = pickerYear === today.getFullYear() && i === today.getMonth()
          return (
            <button
              key={i}
              className={[
                'ym-picker__month',
                isSelected && 'ym-picker__month--selected',
                isCurrent && !isSelected && 'ym-picker__month--current',
              ].filter(Boolean).join(' ')}
              onClick={() => onSelect(pickerYear, i)}
            >
              {name}
            </button>
          )
        })}
      </div>
    </div>
  )
}
