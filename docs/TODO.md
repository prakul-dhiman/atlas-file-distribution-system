# Task TODO

## STEP 1: Apply the fixed files
- [x] client.js — add ngrok-skip-browser-warning header to axios.create() and refresh axios.post
- [x] vite.config.js — add allowedHosts: true
- [x] UserServiceImpl.java — fix localhost:8080 → 8081
- [x] application.yml — fix server.port default 8080 → 8081
- [x] start-public.ps1 — tunnel frontend 5173, set env vars, start frontend
- [x] Final sweep for any remaining localhost:8080/8081 in backend

## STEP 2: Restart everything cleanly
- [x] Stop running backend/frontend/ngrok processes
- [x] docker-compose up -d (postgres 5432 + redis 6379 running)
- [x] Start ngrok on 5173 (frontend) → https://yogurt-grinch-everyone.ngrok-free.dev
- [x] Capture ngrok public URL
- [x] Start backend with env vars pointing to ngrok URL (Started on 8081)
- [x] Start frontend npm run dev (5173)

## STEP 3: Verify
- [x] Backend "Started" log with no errors (Started in 44.569s, Tomcat on 8081)
- [x] Frontend local + network URLs (localhost:5173, 10.246.118.254:5173)
- [x] Open ngrok URL, confirm app loads & login works (health + root serve 200 through tunnel; login 401 for wrong creds = correct)
- [x] Create file share, confirm share link uses ngrok https domain
      → https://yogurt-grinch-everyone.ngrok-free.dev/share/90d16ce7da994a68b9ecc2a977c8fe980df365ff2e314b88ab52b81c699463ba
- [x] Report exact share link

## Email delivery (real SMTP)
- [x] Add spring-boot-starter-mail to pom.xml
- [x] Add Gmail SMTP config to application.yml
- [x] Create SmtpEmailServiceImpl (real sender)
- [x] Make ConsoleEmailServiceImpl fallback (app.mail-enabled=false)
- [x] Restart backend with MAIL_USERNAME / MAIL_PASSWORD
- [x] Verify real email sent (log: "Email sent to prakul5555@gmail.com")
