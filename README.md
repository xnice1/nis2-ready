# NIS2 Ready

NIS2 Ready is a production-style MVP for cybersecurity readiness, gap analysis, remediation tracking, evidence storage, policy preparation, incident checklist management, and JSON reporting for small and medium European companies.

This product is not legal advice, certification, official compliance validation, or a government reporting tool. It provides readiness, maturity, gap analysis, recommended evidence, and items that may need expert review. Confirm legal status and obligations with qualified legal or cybersecurity advisors.

## Tech Stack

- Backend: Java 21, Spring Boot 3, Spring Web, Spring Security, Spring Data JPA, PostgreSQL, Flyway, Maven, JWT, BCrypt, Hibernate Validator
- Frontend: React, TypeScript, Vite, Tailwind CSS, React Router, TanStack Query, React Hook Form, Zod, Recharts
- Infrastructure: Docker Compose, PostgreSQL, local disk evidence storage for development

## Main Features

- Registration and login with JWT authentication and BCrypt password hashing
- Automatic organization creation on registration with the user as `OWNER`
- First/last-name user profile fields and organization member invitations for existing users
- Consultant-client organization relationship table for future consultant dashboards
- Organization-scoped business data across assessments, tasks, evidence, policies, incidents, and reports
- NIS2-style scoping questionnaire with informational results only
- Readiness assessment based on 32 seeded cybersecurity controls
- Scoring by category and overall readiness, with risk levels from `LOW` to `CRITICAL`
- Automatic remediation task generation from weak or missing answers
- Evidence upload with extension validation, checksum calculation, metadata, and safe local storage
- Organization-scoped security audit events for auth, organization, assessment, task, and evidence actions
- Built-in editable policy templates
- Incident response checklist with default actions
- Dashboard and JSON readiness/monthly reports

## Run Locally

From this directory:

```bash
docker compose up --build
```

Services:

- Frontend: http://localhost:5173
- Backend: http://localhost:8080
- PostgreSQL: localhost:5432

Default database settings:

- database: `nis2_ready`
- user: `nis2`
- password: `nis2`

## Backend Environment Variables

- `SPRING_DATASOURCE_URL`: PostgreSQL JDBC URL
- `SPRING_DATASOURCE_USERNAME`: database username
- `SPRING_DATASOURCE_PASSWORD`: database password
- `JWT_SECRET`: signing secret, at least 32 characters for HS256
- `JWT_ISSUER`: expected JWT issuer, default `nis2-ready`
- `EVIDENCE_STORAGE_PATH`: local directory for uploaded evidence files
- `MAX_EVIDENCE_FILE_SIZE_BYTES`: evidence upload size limit, default `10485760`
- `FRONTEND_ORIGIN`: allowed CORS origin, default `http://localhost:5173`

## Frontend Environment Variables

- `VITE_API_BASE_URL`: backend API URL, default `http://localhost:8080/api`

## API Overview

Authentication:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`

Organization:

- `GET /api/organizations/current`
- `PUT /api/organizations/current`
- `GET /api/organizations/members`
- `POST /api/organizations/invite`

Scoping:

- `GET /api/scoping/questions`
- `POST /api/scoping/assess`
- `GET /api/scoping/result`

Assessments:

- `POST /api/assessments`
- `GET /api/assessments`
- `GET /api/assessments/questions`
- `GET /api/assessments/{id}`
- `POST /api/assessments/{id}/answers`
- `POST /api/assessments/{id}/complete`
- `GET /api/assessments/{id}/score`

Audit:

- `GET /api/audit/events?limit=100`

Controls, tasks, evidence, policies, incidents, and reports follow the endpoint list in the product brief.

## Database Migrations

Flyway runs automatically on backend startup.

- `V1__schema.sql`: core schema for users, organizations, memberships, questionnaires, assessments, controls, tasks, files, evidence, policies, incidents, and actions
- `V2__seed_controls_and_templates.sql`: 32 initial controls and 10 policy templates
- `V3__seed_questionnaires.sql`: scoping and readiness questionnaire seed data
- `V4__audit_events.sql`: organization-scoped security audit event history

## Evidence Storage

Development uploads are stored on local disk under `EVIDENCE_STORAGE_PATH`, separated by organization ID. Stored file rows keep a relative storage key and uploader metadata, but API responses expose file IDs and safe metadata, not raw filesystem paths. Supported extensions are PDF, PNG, JPG/JPEG, DOCX, XLSX, CSV, and TXT.

Evidence uploads are treated as confidential. The backend checks organization-scoped access before listing, downloading, changing status, or deleting evidence. Uploads are limited by size, checked against allowed extensions, lightly validated by file signature/content shape, and stored under generated filenames to avoid path traversal.

## Security Notes

- Keep `JWT_SECRET` unique per environment and outside source control.
- Business records are queried by `organizationId`; linked task and assessment IDs are validated before saving.
- `VIEWER` is read-only. Organization updates and invitations require `OWNER` or `ADMIN`.
- Audit events record sensitive workflow activity without storing passwords, JWTs, raw file paths, or file contents.
- Do not put this development local-disk evidence store behind public internet access without adding production storage controls, malware scanning, and backups.

## Screenshots

Placeholder for dashboard, assessment, evidence library, and reports screenshots.

## Roadmap

- PDF report export
- Consultant multi-client dashboard
- ISO 27001 mapping
- NIST CSF mapping
- Supplier questionnaire portal
- Email reminders
- S3-compatible file storage
- Stripe/Paddle billing
- AI-assisted policy drafting with strict disclaimers
- Microsoft 365 and Google Workspace evidence collection

## License

This project is proprietary software. Use, copying, distribution, modification, resale, and hosted commercial operation require prior written permission from xnice1. See [LICENSE](LICENSE).
