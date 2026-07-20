# BloodLink

BloodLink is an emergency blood-request platform that connects hospitals, blood banks, and donors. When a hospital raises an emergency request, BloodLink ranks the best-matched donors using a machine learning service and lets blood banks act on that request in one place — from tracking inventory to notifying donors on Telegram.

The system is split into three services:

| Service | Stack | Purpose |
|---|---|---|
| `backend/` | Java 17, Spring Boot 3, PostgreSQL (Supabase) | REST API for auth, inventory, requests, and dashboards |
| `ml-service/ai-prediction-service/` | Python, Flask, scikit-learn | Ranks eligible donors for an emergency request using a trained Random Forest model |
| `frontend/` | HTML, CSS, vanilla JavaScript | Blood bank control-deck dashboard |

## Why BloodLink

- **AI-ranked donor matching** — donors are scored with a Random Forest classifier trained on distance to the hospital, days since last donation, historical response rate, and request urgency, so blood banks reach the donors most likely to respond first.
- **Live inventory tracking** — blood banks can view and update unit counts per blood group from the dashboard.
- **End-to-end emergency workflow** — hospitals raise a request, the ML service ranks donors, and the blood bank can notify donors directly over Telegram and record donor confirmations.
- **Simple, dependency-light frontend** — a single HTML/CSS/JS dashboard with no build step required.

## Getting Started

### Prerequisites

- Java 17+ and Maven (or use the included `mvnw` wrapper)
- Python 3.11+
- A PostgreSQL database (the project is built against [Supabase](https://supabase.com/))
- A static file server or browser for the frontend (no build tooling required)

### 1. Configure environment variables

Both the backend and the ML service read database and integration credentials from environment variables. Create a `.env` file at the repository root (used by the backend) with:

```env
SUPABASE_HOST=your-db-host
SUPABASE_PORT=5432
SUPABASE_DB=postgres
SUPABASE_USER=your-db-user
SUPABASE_PASSWORD=your-db-password

TELEGRAM_BOT_TOKEN=your-telegram-bot-token
TELEGRAM_TEST_CHAT_ID=your-telegram-chat-id

ML_API_URL=http://localhost:8087
BACKEND_PORT=8080
```

The ML service reads the same database variables from its own `.env` file — see [`ml-service/ai-prediction-service/.env.example`](ml-service/ai-prediction-service/.env.example) for the required keys.

> Never commit real `.env` files. Use the `.env.example` file as a template.

### 2. Run the ML prediction service

```bash
cd ml-service/ai-prediction-service
python -m venv .venv && source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt
python app.py
```

The service starts on `http://localhost:8087`. It exposes:

- `GET /health` — service health check
- `POST /api/ai/rank-donors` — ranks eligible donors for a given `emergency_id`
- `POST /api/ai/train` — retrains the model against the latest database data (falls back to synthetic data when there isn't enough historical data yet)

### 3. Run the backend API

```bash
cd backend
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080` (configurable via `BACKEND_PORT`). Key endpoints:

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/login` | Authenticate a user |
| `GET` | `/api/dashboard` | Blood bank dashboard summary |
| `GET` | `/api/inventory/{bloodBankId}` | Fetch inventory for a blood bank |
| `PUT` | `/api/inventory/{inventoryId}` | Update an inventory record |
| `GET` | `/api/meta/hospitals` | List hospitals for request creation |
| `POST` | `/api/requests` | Create an emergency blood request |
| `GET` | `/api/requests?creatorHospitalId=...` | List requests for a hospital |
| `POST` | `/api/requests/accept` | Confirm a donor's acceptance of a request |
| `POST` | `/api/requests/telegram/send` | Notify a donor via Telegram |

Requests that mutate blood bank data expect an `X-User-Id` header identifying the authenticated blood bank user.

### 4. Run the frontend

The frontend is static and talks to the backend at `http://localhost:8080/api` by default (see `frontend/app.js`). Serve it with any static file server, for example:

```bash
cd frontend
python -m http.server 5500
```

Then open `http://localhost:5500` and log in with a blood bank account.

## Getting Help

- **Bugs and feature requests**: open an issue in this repository's issue tracker.
- **Backend setup details**: see [`backend/HELP.md`](backend/HELP.md) for Spring Boot reference links.
- **Questions**: start a discussion or reach out to a maintainer listed below.

## Maintainers & Contributing

BloodLink is maintained by the project owners listed in the repository's contributor list. Contributions are welcome — please open an issue to discuss significant changes before submitting a pull request, and see `CONTRIBUTING.md` (if present) for coding conventions and the review process.