# 🕵️ FlakeSpy

> Automatically detects flaky tests in your nightly GitLab CI pipeline — no branch push required.

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen?style=flat-square&logo=springboot)
![React](https://img.shields.io/badge/React-18-blue?style=flat-square&logo=react)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-336791?style=flat-square&logo=postgresql)
![Groq AI](https://img.shields.io/badge/Groq-LLaMA3-purple?style=flat-square)
![GitLab](https://img.shields.io/badge/GitLab-CI%2FCD-FC6D26?style=flat-square&logo=gitlab)

---

## 📌 What is FlakeSpy?

Flaky tests are the silent killers of QA confidence. A test that passes sometimes and fails sometimes is worse than no test at all — it trains your team to ignore failures.

**FlakeSpy fixes that — without touching your main branch.**

FlakeSpy connects to your GitLab project using a personal access token, pulls the JUnit XML artifacts from your nightly CI pipeline automatically every morning, and builds a living picture of your test suite health:

- 🏆 **Flakiness leaderboard** — ranked list of your most unreliable tests
- 📈 **Trend tracking** — is your suite getting better or worse over time?
- 🤖 **AI root cause classification** — timing issue? data dependency? locator problem?
- 🔧 **Fix suggestions** — specific, actionable advice per flaky test
- 🔄 **Fully automatic** — no CI changes, no push to main, just a GitLab personal token

---

## 🧱 Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 18, Axios, TailwindCSS, Recharts |
| Backend | Java 17, Spring Boot 3.x |
| AI Engine | Groq API (LLaMA 3) |
| Database | PostgreSQL 15 |
| GitLab Integration | GitLab REST API v4 |
| Scheduler | Spring `@Scheduled` (runs at 6am daily) |
| Build | Maven |
| Containerization | Docker + Docker Compose |

---

## 📁 Project Structure
```
flakespy/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/flakespy/
│   │   │   │   ├── controller/
│   │   │   │   │   ├── GitLabController.java
│   │   │   │   │   ├── TestController.java
│   │   │   │   │   └── DashboardController.java
│   │   │   │   ├── service/
│   │   │   │   │   ├── GitLabService.java
│   │   │   │   │   ├── XmlParserService.java
│   │   │   │   │   ├── FlakinessAnalyserService.java
│   │   │   │   │   ├── GroqAIService.java
│   │   │   │   │   └── NightlyScheduler.java
│   │   │   │   ├── model/
│   │   │   │   │   ├── TestResult.java
│   │   │   │   │   ├── TestRun.java
│   │   │   │   │   └── GitLabProject.java
│   │   │   │   ├── repository/
│   │   │   │   │   ├── TestResultRepository.java
│   │   │   │   │   ├── TestRunRepository.java
│   │   │   │   │   └── GitLabProjectRepository.java
│   │   │   │   ├── dto/
│   │   │   │   │   ├── GitLabConnectRequest.java
│   │   │   │   │   ├── FlakyTestSummary.java
│   │   │   │   │   └── TestAnalysis.java
│   │   │   │   └── config/
│   │   │   │       └── CorsConfig.java
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── schema.sql
│   └── pom.xml
│
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   │   ├── GitLabConnect.jsx
│   │   │   ├── FlakyLeaderboard.jsx
│   │   │   ├── TrendChart.jsx
│   │   │   ├── RootCauseCard.jsx
│   │   │   └── SuiteHealthBadge.jsx
│   │   ├── pages/
│   │   │   ├── Dashboard.jsx
│   │   │   └── TestDetail.jsx
│   │   ├── api/
│   │   │   └── flakespyApi.js
│   │   ├── utils/
│   │   │   └── flakinessScore.js
│   │   ├── App.jsx
│   │   └── main.jsx
│   ├── public/
│   └── package.json
│
├── docker-compose.yml
├── .gitignore
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Node.js 18+
- PostgreSQL 15+ (or use Docker)
- Maven 3.8+
- A free [Groq API key](https://console.groq.com)
- A GitLab personal access token with `read_api` scope

### 1. Generate your GitLab personal access token

1. Log in to GitLab
2. Go to **Settings → Access Tokens**
3. Create a token with `read_api` scope
4. Copy the token — you'll paste it into FlakeSpy on first launch

> **No admin access needed. No push permissions needed. Read-only is enough.**

### 2. Clone the repository
```bash
git clone https://github.com/kevilkhadka/flakespy.git
cd flakespy
```

### 3. Set up the database
```bash
psql -U postgres -c "CREATE DATABASE flakespy;"
```

### 4. Configure the backend

Edit `backend/src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/flakespy
    username: postgres
    password: your_password_here

groq:
  api:
    key: your_groq_api_key_here
    url: https://api.groq.com/openai/v1/chat/completions
    model: llama3-8b-8192

flakespy:
  scheduler:
    cron: "0 0 6 * * *"
```

### 5. Run the backend
```bash
cd backend
mvn spring-boot:run
```

### 6. Run the frontend
```bash
cd frontend
npm install
npm run dev
```

### 7. Connect your GitLab project

Open `http://localhost:5173`, click **Connect GitLab**, and enter:
- Your GitLab project ID (found in **Settings → General**)
- Your personal access token
- The CI job name that produces XML artifacts (e.g. `test`, `run-tests`)

### 8. (Optional) Run with Docker
```bash
docker-compose up --build
```

---

## 🔌 API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/gitlab/connect` | Save GitLab token + project ID |
| `POST` | `/api/gitlab/sync` | Manually trigger artifact pull now |
| `GET` | `/api/tests/flaky` | Get flakiness leaderboard (top 20) |
| `GET` | `/api/tests/{name}/history` | Full 30-night history for one test |
| `GET` | `/api/tests/{name}/analysis` | AI root cause + fix suggestions |
| `GET` | `/api/dashboard/summary` | Overall suite health stats |

### Example — Leaderboard response
```json
[
  {
    "testName": "loginWithGoogleOAuthTest",
    "className": "com.app.tests.AuthTest",
    "flakinessScore": 0.57,
    "totalRuns": 30,
    "failures": 17,
    "lastFailure": "2025-03-13T02:14:33",
    "failurePattern": "Fails consistently between 2am-3am",
    "rootCause": "TIMING",
    "fixSuggestion": "Replace Thread.sleep(2000) with explicit wait for element visibility"
  }
]
```

---

## 🔍 How the GitLab API integration works
```
GET /api/v4/projects/:id/jobs?scope=success
Authorization: Bearer YOUR_PERSONAL_TOKEN
```

Then for each nightly job:
```
GET /api/v4/projects/:id/jobs/:job_id/artifacts
```

Downloads the artifact ZIP, extracts JUnit XML, feeds into the parser. `NightlyScheduler` runs this at 6am daily — after your nightly pipeline finishes.

---

## 🧠 Flakiness score explained
```
flakiness_score = number_of_status_flips / (total_runs - 1)
```

| Score | Label | Action |
|---|---|---|
| 0.0 – 0.2 | Stable | No action needed |
| 0.2 – 0.5 | Unstable | Monitor closely |
| 0.5 – 0.8 | Flaky | Fix soon |
| 0.8 – 1.0 | Broken | Fix immediately |

---

## 🤖 AI root cause categories

| Category | Typical symptom |
|---|---|
| `TIMING` | `TimeoutException`, `StaleElementException` |
| `DATA_DEPENDENCY` | Fails after another test runs, passes in isolation |
| `LOCATOR` | `NoSuchElementException` intermittently |
| `NETWORK` | Connection timeouts, SauceLabs session drops |
| `CONCURRENCY` | Fails in parallel runs only |
| `ENVIRONMENT` | Fails on specific device/OS only |

---

## 🗺️ Roadmap

- [x] Project setup and architecture design
- [ ] Spring Boot skeleton + PostgreSQL
- [ ] GitLab API integration
- [ ] JUnit XML parser
- [ ] Flakiness score engine
- [ ] NightlyScheduler
- [ ] Groq AI root cause classification
- [ ] REST API endpoints
- [ ] React dashboard
- [ ] GitLab connect UI
- [ ] Test detail page
- [ ] Docker Compose
- [ ] Allure JSON support (Phase 2)

---

## 👤 Author

**Kevil** — QA Engineer learning full-stack development  

---

## 📄 License

MIT License

---

> 💡 **Part of the QA Tools ecosystem:**
> - [TestGenie AI](https://github.com/YOUR_USERNAME/testgenie-ai) — AI-powered test case generator
