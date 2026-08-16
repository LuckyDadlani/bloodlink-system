# BloodLink AI - Phase 1 Foundation

This workspace now contains:

- `backend/`: Spring Boot API for dashboard, inventory, manual requests, donor notifications, and donor acceptance.
- `ml-service/ai-prediction-service/`: Flask ML ranking service (`/api/ai/rank-donors`).

## 1) Backend Setup

Create environment variables before running `backend`:

```bash
export SUPABASE_HOST=aws-1-ap-southeast-2.pooler.supabase.com
export SUPABASE_PORT=5432
export SUPABASE_DB=postgres
export SUPABASE_USER=postgres.vllitwzpfefkeedddslf
export SUPABASE_PASSWORD=bloodlink_ai_13

export TELEGRAM_BOT_TOKEN=8645117226:AAG1LQwUZN0jS5Mv6iA2N7tN3Wf6ATOcFZU
export TELEGRAM_TEST_CHAT_ID=5339809045

export ML_SERVICE_BASE_URL=http://localhost:8087
export ML_TOP_N_DONORS=5
```

Run:

```bash
cd backend
mvn spring-boot:run
```

## 2) ML Service Setup (with venv)

The ML repository has been cloned into `ml-service/` and dependencies added.

Run:

```bash
cd ml-service/ai-prediction-service
python3.11 -m venv .venv
. .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
python app.py
```

The service starts on port `8087`.

## 3) Implemented APIs (Backend)

### Authentication

- `POST /api/auth/login`
- Body:

```json
{
	"email": "bloodbank@...",
	"password": "..."
}
```

Response includes blood bank context (`userId`, `bloodBankId`, `bloodBankName`) used by the dashboard frontend.

### Metadata

- `GET /api/meta/hospitals`
- Used by frontend to select `creatorHospitalId` while creating and filtering requests.

### Dashboard

- `GET /api/dashboard?bloodBankId=<uuid>&creatorHospitalId=<uuid>`
- Header required: `X-User-Id: <uuid>` (must be active `BLOOD_BANK` user)

### Inventory

- `GET /api/inventory/{bloodBankId}`
- Header required: `X-User-Id`

- `PUT /api/inventory/{inventoryId}`
- Header required: `X-User-Id`
- Body:

```json
{
	"updatedBy": "<uuid>",
	"unitsAvailable": 25
}
```

### Emergency Requests

- `POST /api/requests`
- Header required: `X-User-Id`
- Body:

```json
{
	"creatorHospitalId": "<uuid>",
	"bloodGroupRequired": "A+",
	"componentRequired": "Whole Blood",
	"unitsRequired": 2,
	"urgencyLevel": "HIGH"
}
```

Flow on create:

1. inserts in `emergency_requests`
2. calls ML `/api/ai/rank-donors`
3. falls back to local ranking if ML unavailable
4. inserts top-N into `donor_notifications`
5. sends Telegram message with accept action context

- `GET /api/requests?creatorHospitalId=<uuid>`
- Header required: `X-User-Id`

### Donor Acceptance

- `POST /api/requests/accept`
- Body:

```json
{
	"emergencyId": "<uuid>",
	"donorId": "<uuid>"
}
```

This is atomic and only first donor acceptance succeeds.

### Telegram Send (manual test)

- `POST /api/requests/telegram/send`
- Header required: `X-User-Id`
- Body:

```json
{
	"message": "Test from BloodLink",
	"chatId": "5339809045"
}
```

## 4) Notes

- Request status transitions currently use existing enum values in Supabase (`CREATED`, `DONORS_NOTIFIED`, `DONOR_CONFIRMED`, etc.).
- Credentials are externalized in `backend/src/main/resources/application.properties`.
- Donor acceptance is conflict-safe via single conditional SQL update.

## 5) Frontend Dashboard

The frontend is implemented in `frontend/` as a SPA with:

- BLOOD_BANK login screen
- inventory table + edit action
- emergency request creation form
- recent requests table
- summary KPI cards
- auto-refresh every 10 seconds

Run locally:

```bash
cd frontend
python3 -m http.server 5173
```

Open `http://localhost:5173`.
