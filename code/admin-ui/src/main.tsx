import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App'
import { ToastProvider } from './components/ui'
import { initAuth } from './auth'
import './styles.css'

// Finish the Keycloak redirect (if any) before the first render
initAuth().then(authError => {
  createRoot(document.getElementById('root')!).render(
    <StrictMode><BrowserRouter><ToastProvider><App authError={authError} /></ToastProvider></BrowserRouter></StrictMode>,
  )
})
