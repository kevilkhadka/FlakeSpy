import { Routes, Route } from 'react-router-dom'
import Dashboard from './pages/Dashboard'
import TestDetail from './pages/TestDetail'

function App() {
  return (
    <Routes>
      <Route path="/"                  element={<Dashboard />} />
      <Route path="/test/:testName"    element={<TestDetail />} />
    </Routes>
  )
}

export default App
