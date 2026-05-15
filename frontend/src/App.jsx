import CalendarPage from './pages/CalendarPage'
import './App.css'

export default function App() {
  return (
    <div className="app">
      <nav className="app-nav">
        <span className="app-logo">briefy</span>
      </nav>
      <main>
        <CalendarPage />
      </main>
    </div>
  )
}
