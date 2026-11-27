# Rikkei Jobs frontend

React, TypeScript, Vite, and Tailwind CSS frontend for the Rikkei Jobs platform.
AI Career is integrated into the authenticated candidate area:

- `/candidate/career` — career overview
- `/candidate/career/recommendations` — job recommendations
- `/candidate/career/skills` — skill-gap analysis

Career requests use the signed-in candidate's access token and are proxied to
AI Career Service. They do not use mock recommendations or a manually entered
token.

## Local development

From the repository root, copy `.env.example` to `.env` and set values for
your local services. The root `.env` is ignored by git.

```powershell
cd frontend
npm install
npm run dev
```

Vite reads `VITE_BACKEND_URL` and `VITE_AI_CAREER_URL` from the root `.env` (or
`frontend/.env`) to configure the `/api`, `/lms`, `/staff`, and `/ai-career`
development proxies. Local defaults are provided.

## Validation

```powershell
npm run build
npm run lint
```
