# Orizon · Assistente per la stima della CO₂ dei viaggi

Chat con un LLM (function calling) che raccoglie **mezzo, peso bagagli, origine, destinazione** e calcola la CO₂ tramite l'API EcoFreight.

- `backend/` — Java 17, Spring Boot 3. Un solo endpoint `POST /api/chat`, system prompt, tool `calculate_co2` → EcoFreight, memoria persistente su DB (JPA).
- `frontend/` — React (Vite), UI in stile chat.

## Avvio in locale

Chiavi necessarie (variabili d'ambiente, mai nel codice):

| Variabile | Descrizione |
|---|---|
| `LLM_API_KEY` | chiave di un LLM con API compatibile OpenAI (default: Groq, piano gratuito, https://console.groq.com) |
| `LLM_BASE_URL` / `LLM_MODEL` | opzionali, per usare OpenAI, Gemini, OpenRouter… |
| `ECOFREIGHT_API_KEY` | chiave gratuita (1000 richieste/mese) da https://ecofreight.co |

Requisiti: Java 17, Maven, Node 18+.

Backend: copia `backend/secrets.properties.example` in `backend/secrets.properties` e inserisci le tue due chiavi (il file è ignorato da Git). In alternativa usa le variabili d'ambiente.
```powershell
cd backend
copy secrets.properties.example secrets.properties   # poi modificalo
mvn spring-boot:run                                    # http://localhost:8080
```
Con variabili d'ambiente al posto del file: `$env:LLM_API_KEY="..."; $env:ECOFREIGHT_API_KEY="..."`.
Senza chiavi il backend parte, ma la chat risponde con un errore di servizio non disponibile.

Frontend:
```powershell
cd frontend
npm install
npm run dev     # http://localhost:5173
```

## API
- `POST /api/chat` `{ "conversationId": "<opz.>", "message": "..." }` → `{ "conversationId", "reply" }`
- `GET /api/chat/{conversationId}/messages` → storico (la chat si ricarica dopo un refresh)

## Memoria
Ogni messaggio utente/assistente è salvato in tabella `chat_message`. A ogni richiesta gli ultimi 30 messaggi della conversazione vengono reinviati all'LLM. Locale: H2 su file (`backend/data`). Produzione: imposta `SPRING_DATASOURCE_URL` (+ `_USERNAME`, `_PASSWORD`) verso PostgreSQL gratuito (Neon/Supabase/Render), perché il disco di Render free è effimero.

## Deploy (gratuito)
- **Backend → Render** (Web Service, Docker o runtime Java): build `mvn -DskipTests package`, start `java -jar target/co2-agent-1.0.0.jar`. Variabili: `LLM_API_KEY`, `ECOFREIGHT_API_KEY`, `FRONTEND_ORIGIN` (URL Netlify), `SPRING_DATASOURCE_*`.
- **Frontend → Netlify**: base `frontend`, build `npm run build`, publish `dist`, variabile `VITE_API_URL` = URL del backend Render.

## Note
- EcoFreight è un'API di *trasporto merci*: la stima si basa su peso × distanza con un veicolo predefinito per modalità (strada: furgone, treno: diesel, nave: portacontainer, aereo: cargo in stiva). Il system prompt la presenta come stima.
- La UI è un chat custom in React; si può sostituire con i componenti di shadcn.io/ai mantenendo le stesse chiamate API.
