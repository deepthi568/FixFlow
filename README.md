# FixFlow

FixFlow is a repair service management application for tracking repair tickets, assigning technicians, managing spare-part stock, monitoring service deadlines, and reviewing administrative audit history.

## Features

- JWT-based sign-in and sign-out with ADMIN, SERVICE_STAFF, and TECHNICIAN roles.
- Ticket intake, search, filters, status tracking, technician assignment, priority, and target completion dates.
- Dashboard counts for ticket status, overdue/approaching deadlines, technician workload, and low-stock inventory.
- Technician profile management and ticket assignment.
- Inventory stock adjustment and part consumption against tickets, with movement history.
- Administrator audit trail for ticket, assignment, and inventory actions.
- Analytics/report pages based on persisted ticket records.

SLA alerts are displayed in the application. External email or push notifications are not configured.

## Architecture

| Part | Technology | Local address |
| --- | --- | --- |
| Frontend | React 19, Vite, React Router, Axios | `http://localhost:5173` |
| Backend | Java 17, Spring Boot 3.5, Spring Security, Spring Data JPA, REST | `http://localhost:8080` |
| Database | MySQL | `localhost:3306` |

The React client calls the Spring REST API. Spring Security verifies bearer JWTs and enforces roles; Spring Data JPA persists application records to MySQL. Automated backend tests use an isolated in-memory H2 database instead of the local MySQL database.

## Project structure

```text
FixFlow/
├── .github/workflows/ci.yml
├── backend/
│   └── backend/                 # Spring Boot Maven project (intentionally nested)
│       ├── config/checkstyle.xml
│       ├── scripts/              # Demo-data seeder
│       ├── src/main/java/        # Controllers, services, entities, repositories, security
│       ├── src/main/resources/   # Spring configuration templates
│       ├── src/test/java/        # Unit and integration tests
│       ├── application-local.properties  # Local-only; ignored by Git
│       └── pom.xml
├── frontend/
│   ├── src/components/           # Shared navigation, forms, and UI components
│   ├── src/pages/                # Dashboard, tickets, inventory, reports, admin pages
│   ├── src/__tests__/            # React component tests
│   ├── package.json
│   └── vite.config.js
├── .env.example                  # Placeholder values only; not loaded automatically
├── DEMO_DATA.md
├── PHASE2_QA.md
└── SECURITY_SETUP.md
```

## Prerequisites

- Java Development Kit 17.
- Apache Maven 3.9 or later. The project also includes Maven Wrapper scripts and pins Maven in `.mvn/wrapper/maven-wrapper.properties`.
- Node.js 22 (or Node 20.19 or later) and npm.
- MySQL 8.x running locally for persisted development data.

Docker Compose is not included: Docker is installed in the validation environment, but its daemon is unavailable, so a Compose setup could not be tested there.

## Database setup

Connect to MySQL as an administrator (for example, `mysql -u root -p`) and create the local database and a dedicated application account. Replace the password placeholder with a unique password; don’t commit or share it.

```sql
CREATE DATABASE IF NOT EXISTS fixflow;
CREATE USER IF NOT EXISTS 'fixflow_app'@'localhost'
  IDENTIFIED BY 'replace-with-a-strong-local-password';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
  ON fixflow.* TO 'fixflow_app'@'localhost';
```

If that account already exists, use `ALTER USER 'fixflow_app'@'localhost' IDENTIFIED BY 'your-new-local-password';` to set its password before granting access. Hibernate uses `spring.jpa.hibernate.ddl-auto=update` to create/update the schema at backend startup; keep a database backup before applying schema changes to an existing database.

## Local configuration

Copy the safe properties template to the path expected by Spring Boot:

```powershell
Copy-Item backend/backend/src/main/resources/application-local.properties.example `
  backend/backend/application-local.properties
```

Edit `backend/backend/application-local.properties` and replace every `replace-with-...` value. The backend imports this file relative to its working directory, so run Maven from `backend/backend`. The file is ignored by Git. Do not put secrets in `src/main/resources`.

The root `.env.example` documents equivalent environment variables but is **not** automatically loaded by Spring Boot or Vite. You can set these in your shell/IDE run configuration instead of using the local properties file:

| Variable | Purpose |
| --- | --- |
| `MYSQL_URL` | JDBC URL, normally `jdbc:mysql://localhost:3306/fixflow` |
| `MYSQL_USER`, `MYSQL_PASSWORD` | Dedicated MySQL application account |
| `JWT_SECRET` | Random signing secret with at least 32 bytes of data |
| `JWT_EXPIRATION_MS` | Token lifetime in milliseconds (default 28,800,000) |
| `FIXFLOW_ADMIN_USERNAME`, `FIXFLOW_ADMIN_PASSWORD` | First administrator credentials; password must be at least 8 characters |
| `FIXFLOW_ADMIN_FULL_NAME` | Display name for initial administrator |
| `CORS_ALLOWED_ORIGINS` | Comma-separated frontend origins; local default is `http://localhost:5173` |
| `FIXFLOW_SLA_APPROACHING_DAYS` | Deadline warning window in days (default 2) |
| `VITE_API_BASE_URL` | Backend API base URL used when building/running the frontend (default `http://localhost:8080`) |

Generate a JWT signing secret locally, for example with `node -e "console.log(require('crypto').randomBytes(48).toString('base64'))"`, and paste it directly into your ignored local config. Don’t send it in chat or commit it.

The initial administrator is created only when its configured username does not already exist. Changing the bootstrap password later does not reset an existing account. Administrators create service-staff and technician login accounts in the Users page. See [SECURITY_SETUP.md](SECURITY_SETUP.md) for role and authentication details.

## Run the application

Open two terminals.

Backend, from the nested Maven project directory:

```powershell
cd backend/backend
mvn spring-boot:run
```

Frontend:

```powershell
cd frontend
npm ci
npm run dev
```

Open `http://localhost:5173`. Use the configured **application administrator** credentials to sign in; these are separate from the MySQL username/password. Keep the backend running while using the frontend. Use `localhost` consistently because the default CORS origin is `http://localhost:5173`.

## Tests, lint, and builds

Backend, from `backend/backend`:

```powershell
mvn -Dspring.profiles.active=test test
mvn checkstyle:check
```

The test profile uses H2 with create/drop schema management and seeded test fixtures; it does not connect to the configured development MySQL database. Checkstyle currently reports legacy wildcard-import style findings while allowing the build to continue.

Frontend, from `frontend`:

```powershell
npm ci
npm test
npm run lint
npm run format:check
npm run build
```

The CI workflow runs backend `verify`, frontend tests, lint/format checks, and the production frontend build on pushes and pull requests. `npm run format:check` currently checks the test and configuration files; `npm run format` formats the frontend source.

For a fictional, persistent demonstration dataset, follow [DEMO_DATA.md](DEMO_DATA.md). The seeder writes records through the authenticated API and marks each one as demo data.

## Troubleshooting

- **Login says backend cannot be reached:** start Spring Boot and confirm it started on port 8080. Review the backend console for MySQL connection errors.
- **MySQL access denied:** check `spring.datasource.username` and `spring.datasource.password` against the MySQL account and its grants on `fixflow.*`. Environment variables `MYSQL_USER`/`MYSQL_PASSWORD` can override the local values.
- **Login says invalid credentials:** use `fixflow.admin.username`/`fixflow.admin.password`, not the MySQL account. Bootstrap does not reset existing administrator passwords.
- **Browser reports CORS:** serve the frontend at the exact origin in `CORS_ALLOWED_ORIGINS`; by default that is `http://localhost:5173`. Avoid switching between `localhost` and `127.0.0.1` unless both are allowed.
- **Port already in use:** stop the other app using port 8080 or 5173 before starting another instance.
- **Frontend dependencies fail:** use Node 22 and run `npm ci` from `frontend`.

## GitHub submission

The project root currently has no `.git` repository. To submit it, create a **private** empty repository in GitHub’s UI. Don’t select GitHub options to create another README, `.gitignore`, or license because those files are already local. Then inspect the local files before the first commit:

```powershell
git init -b main
git status --short --ignored
git add .
git status --short
git commit -m "Prepare FixFlow Phase 2 submission"
git remote add origin https://github.com/YOUR-ACCOUNT/YOUR-PRIVATE-REPO.git
git push -u origin main
```

Before committing, verify that `application-local.properties`, `.env`, `target/`, `node_modules/`, and `dist/` do not appear as staged files. This project has not been published or deployed by these setup instructions.
