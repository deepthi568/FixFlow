# FixFlow authentication setup

FixFlow uses Spring Security with short-lived signed JWT access tokens. The React client keeps the token in `sessionStorage`; logout revokes its token ID in MySQL, and the server also rejects expired tokens. Passwords for users created through the admin API and the initial-admin bootstrap are encoded with BCrypt.

## Local configuration

Create the `fixflow` MySQL database, then copy `backend/backend/src/main/resources/application-local.properties.example` to `backend/backend/application-local.properties`. Replace every `replace-with-...` value with a unique local value. The local file is ignored by Git. Alternatively provide `MYSQL_URL`, `MYSQL_USER`, `MYSQL_PASSWORD`, `JWT_SECRET`, `FIXFLOW_ADMIN_USERNAME`, and `FIXFLOW_ADMIN_PASSWORD` in the backend process environment. `JWT_SECRET` must contain at least 32 bytes of random data; the application refuses to start if it is missing or too short.

Set `CORS_ALLOWED_ORIGINS` to the frontend origin when it differs from `http://localhost:5173`. For a deployed frontend, set `VITE_API_BASE_URL` at frontend build time to the backend origin.

When `FIXFLOW_ADMIN_USERNAME` and `FIXFLOW_ADMIN_PASSWORD` are set, startup creates that administrator only if the username does not exist. The password must have at least 8 characters. No default account or password is created. Remove those bootstrap environment variables after the first administrator has been created.

## Run locally

From `backend/backend`, run `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`). From `frontend`, run `npm run dev`. Open the frontend and sign in with the administrator credentials you configured. Administrators create service staff and technician accounts through the existing `/api/users` endpoint; set `technicianId` for a technician account to link it to its technician record.

All protected API requests require `Authorization: Bearer <token>`. The backend enforces roles and technician ticket assignment. A technician can read only assigned tickets and can update repair progress to `DIAGNOSED`, `IN_REPAIR`, `QUALITY_CHECK`, `ON_HOLD`, or `READY`; completing, cancelling, assigning, or editing ticket details remains outside technician permissions.
