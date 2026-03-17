# 🕵️ FlakeSpy

> Automatically detects flaky tests in your nightly GitLab CI pipeline — no branch push required.

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.3-brightgreen?style=flat-square&logo=springboot)
![React](https://img.shields.io/badge/React-18-blue?style=flat-square&logo=react)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-336791?style=flat-square&logo=postgresql)
![Groq AI](https://img.shields.io/badge/Groq-LLaMA3-purple?style=flat-square)
![GitLab](https://img.shields.io/badge/GitLab-CI%2FCD-FC6D26?style=flat-square&logo=gitlab)
![Vite](https://img.shields.io/badge/Vite-5.x-646CFF?style=flat-square&logo=vite)

---

## 📌 What is FlakeSpy?

Flaky tests are the silent killers of QA confidence. A test that passes sometimes and fails sometimes is worse than no test at all — it trains your team to ignore failures.

**FlakeSpy fixes that — without touching your main branch.**

FlakeSpy connects to your GitLab project using a personal access token, pulls the JUnit XML artifacts from your nightly CI pipeline automatically every morning, and builds a living picture of your test suite health:

- 🏆 **Flakiness leaderboard** — ranked list of your most unreliable tests
- 📈 **30-night trend chart** — pass/fail history + duration over time
- 🤖 **AI root cause classification** — timing? locator? data dependency?
- 🔧 **Fix suggestions** — specific, actionable advice per flaky test
- 🔄 **Fully automatic** — no CI changes, no push to main, just a GitLab personal token
- 🔍 **Test detail page** — deep dive into any test's full history and AI analysis

---

## 🧱 Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 18, Vite 5, Axios, Recharts |
| Backend | Java 17, Spring Boot 3.2.3 |
| AI Engine | Groq API (LLaMA 3) |
| Database | PostgreSQL 15 |
| GitLab Integration | GitLab REST API v4 |
| Scheduler | Spring `@Scheduled` (runs at 6am daily) |
| XML Parsing | JAXB / Java DOM Parser |
| ZIP Extraction | Apache Commons Compress |
| Build | Maven |

---

## 📁 Project Structure
```
flakespy/
├── backend/
│   ├── src/main/java/com/flakespy/
│   │   ├── controller/
│   │   │   ├── GitLabController.java
│   │   │   ├── TestController.java
│   │   │   └── DashboardController.java
│   │   ├── service/
│   │   │   ├── GitLabService.java
│   │   │   ├── XmlParserService.java
│   │   │   ├── FlakinessAnalyserService.java
│   │   │   ├── GroqAIService.java
│   │   │   └── NightlyScheduler.java
│   │   ├── model/
│   │   │   ├── GitLabProject.java
│   │   │   ├── TestRun.java
│   │   │   └── TestResult.java
│   │   ├── repository/
│   │   │   ├── GitLabProjectRepository.java
│   │   │   ├── TestRunRepository.java
│   │   │   └── TestResultRepository.java
│   │   ├── dto/
│   │   │   ├── GitLabConnectRequest.java
│   │   │   ├── FlakyTestSummary.java
│   │   │   ├── TestAnalysis.java
│   │   │   └── DashboardSummary.java
│   │   ├── config/
│   │   │   └── CorsConfig.java
│   │   └── FlakespyApplication.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── application-local.yml   ← gitignored, your secrets
│   └── pom.xml
│
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   │   ├── GitLabConnect.jsx
│   │   │   ├── SuiteHealthBadge.jsx
│   │   │   ├── FlakyLeaderboard.jsx
│   │   │   └── TrendChart.jsx
│   │   ├── pages/
│   │   │   ├── Dashboard.jsx
│   │   │   └── TestDetail.jsx
│   │   ├── api/
│   │   │   └── flakespyApi.js
│   │   ├── App.jsx
│   │   ├── main.jsx
│   │   └── index.css
│   └── package.json
│
├── .gitignore
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Node.js 20+
- PostgreSQL 15+
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
git clone https://github.com/YOUR_USERNAME/flakespy.git
cd flakespy
```

### 3. Set up the database
```bash
psql -U your_username -d postgres -c "CREATE DATABASE flakespy;"
```

### 4. Configure the backend

Create `backend/src/main/resources/application-local.yml` — this file is gitignored:
```yaml
DB_USERNAME: your_postgres_username
DB_PASSWORD:
GROQ_API_KEY: your_groq_api_key
```

### 5. Run the backend
```bash
cd backend
mvn spring-boot:run
```

Spring Boot auto-creates all 3 PostgreSQL tables on first run. Backend starts on `http://localhost:8080`.

### 6. Run the frontend
```bash
cd frontend
npm install
npm run dev
```

Frontend starts on `http://localhost:5173`.

### 7. Connect your GitLab project

Open `http://localhost:5173` and fill in the connect form:
- **GitLab project ID** — found in GitLab → Settings → General
- **Project name** — any display name you choose
- **Personal access token** — the `read_api` token you created
- **CI job name** — the job in your `.gitlab-ci.yml` that produces XML artifacts
- **GitLab URL** — `https://gitlab.com` or your self-hosted URL

FlakeSpy immediately pulls your last 30 nightly runs and builds your first flakiness report.

---

## 🔌 API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/gitlab/connect` | Connect a GitLab project |
| `POST` | `/api/gitlab/sync/{id}` | Manually trigger artifact pull |
| `GET` | `/api/gitlab/projects` | List connected projects |
| `DELETE` | `/api/gitlab/projects/{id}` | Disconnect a project |
| `GET` | `/api/tests/flaky?projectId=1` | Flakiness leaderboard |
| `GET` | `/api/tests/{name}/history` | 30-night pass/fail history |
| `GET` | `/api/tests/{name}/analysis` | AI root cause + fix suggestion |
| `GET` | `/api/dashboard/summary?projectId=1` | Suite health summary |

---

## 🔍 How the GitLab API integration works

FlakeSpy uses the **GitLab REST API v4** — no pipeline changes needed:
```
GET /api/v4/projects/:id/jobs?scope=success
Authorization: Bearer YOUR_PERSONAL_TOKEN
```

Then downloads artifacts for each nightly job:
```
GET /api/v4/projects/:id/jobs/:job_id/artifacts
```

`NightlyScheduler` runs this automatically every morning at 6am after your nightly pipeline finishes. You can also trigger a manual sync anytime from the dashboard.

---

## 🧠 Flakiness score explained
```
flakiness_score = status_flips / (total_runs - 1)
```

| Score | Label | Action |
|---|---|---|
| 0.0 – 0.2 | 🟢 Stable | No action needed |
| 0.2 – 0.5 | 🟡 Unstable | Monitor closely |
| 0.5 – 0.8 | 🟠 Flaky | Fix soon |
| 0.8 – 1.0 | 🔴 Broken | Fix immediately |

---

## 🤖 AI root cause categories

| Category | Typical symptom |
|---|---|
| `TIMING` | `TimeoutException`, `StaleElementException` |
| `LOCATOR` | `NoSuchElementException` intermittently |
| `DATA_DEPENDENCY` | Fails after another test, passes in isolation |
| `NETWORK` | Connection timeouts, session drops |
| `CONCURRENCY` | Fails in parallel runs only |
| `ENVIRONMENT` | Fails on specific device/OS only |

---

## 🗺️ Roadmap

- [x] Project setup and architecture
- [x] Spring Boot REST API
- [x] PostgreSQL — 3 tables auto-created
- [x] GitLab API integration
- [x] JUnit XML parser
- [x] Flakiness score algorithm
- [x] Nightly scheduler (6am auto-sync)
- [x] Groq AI root cause classification
- [x] React frontend
- [x] GitLab connect form
- [x] Suite health dashboard
- [x] Flakiness leaderboard with filters
- [x] 30-night trend chart
- [x] Test detail + AI analysis page
- [ ] Allure JSON format support
- [ ] Email alerts for new broken tests
- [ ] Multi-project support

---

## 👤 Author

**Kevil** — QA Engineer learning full-stack development
Project 2 of my coding journey | Built from a real pain I face every single night

---

## 📄 License

MIT License

---

> 💡 **Part of the QA Tools ecosystem:**
> - [TestGenie AI](https://github.com/YOUR_USERNAME/testgenie-ai) — AI-powered test case generator
