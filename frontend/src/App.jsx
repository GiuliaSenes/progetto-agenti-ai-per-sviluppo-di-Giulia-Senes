import { useEffect, useRef, useState } from 'react'
import ReactMarkdown from 'react-markdown'
import './App.css'

const API = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'
const STORAGE_KEY = 'orizon-conversation-id'

const WELCOME = {
  role: 'assistant',
  content:
    'Ciao! Sono l\'assistente di **Orizon**. Dimmi come viaggi, da dove a dove e quanto pesano i bagagli, e stimo la CO₂ del tuo viaggio.',
}

export default function App() {
  const [conversationId, setConversationId] = useState(() => localStorage.getItem(STORAGE_KEY))
  const [messages, setMessages] = useState([WELCOME])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const bottomRef = useRef(null)

  // Recupero dello storico dal backend dopo un refresh
  useEffect(() => {
    if (!conversationId) return
    fetch(`${API}/api/chat/${conversationId}/messages`)
      .then((r) => (r.ok ? r.json() : []))
      .then((hist) => hist.length && setMessages([WELCOME, ...hist]))
      .catch(() => {})
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, loading])

  async function send(e) {
    e.preventDefault()
    const text = input.trim()
    if (!text || loading) return
    setInput('')
    setMessages((m) => [...m, { role: 'user', content: text }])
    setLoading(true)
    try {
      const res = await fetch(`${API}/api/chat`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ conversationId, message: text }),
      })
      const data = await res.json()
      if (!res.ok) throw new Error(data.error || 'Errore del server')
      if (data.conversationId !== conversationId) {
        setConversationId(data.conversationId)
        localStorage.setItem(STORAGE_KEY, data.conversationId)
      }
      setMessages((m) => [...m, { role: 'assistant', content: data.reply }])
    } catch (err) {
      setMessages((m) => [...m, { role: 'assistant', content: `⚠️ ${err.message}`, error: true }])
    } finally {
      setLoading(false)
    }
  }

  function newChat() {
    localStorage.removeItem(STORAGE_KEY)
    setConversationId(null)
    setMessages([WELCOME])
  }

  return (
    <div className="app">
      <header>
        <div>
          <h1>🌍 Orizon · Calcolatore CO₂</h1>
          <p>Viaggia con consapevolezza</p>
        </div>
        <button className="ghost" onClick={newChat}>Nuova chat</button>
      </header>

      <main>
        {messages.map((m, i) => (
          <div key={i} className={`row ${m.role}`}>
            <div className={`bubble ${m.error ? 'error' : ''}`}>
              <ReactMarkdown>{m.content}</ReactMarkdown>
            </div>
          </div>
        ))}
        {loading && (
          <div className="row assistant">
            <div className="bubble typing"><span /><span /><span /></div>
          </div>
        )}
        <div ref={bottomRef} />
      </main>

      <form onSubmit={send}>
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Es. Vado in aereo da Roma a Tokyo con 20 kg di bagaglio"
          maxLength={2000}
          autoFocus
        />
        <button type="submit" disabled={loading || !input.trim()}>Invia</button>
      </form>
    </div>
  )
}
