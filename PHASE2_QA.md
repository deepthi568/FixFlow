# FixFlow Phase 2 QA Guide

## Automated checks

Run the backend suite from `backend/backend`:

```powershell
mvn -Dspring.profiles.active=test test
```

The tests use `src/test/resources/application-test.properties`, H2 in MySQL mode, and `create-drop` schema management. Test data is created and cleared by the test classes. This profile does not use the local MySQL properties or connect to the development database.

Run frontend static analysis and production build from `frontend`:

```powershell
npm run lint
npm run format:check
npm test
npm run build
```

The backend tests cover ticket creation and required-field validation, status changes, search and filters, login and invalid credentials, role-based restrictions, technician assignment, inventory restock and consumption (including insufficient stock), and audit event creation and administrator access. SLA calculations are tested separately. Frontend tests exercise required login inputs, rejected-login error messaging, and role-specific navigation.

## Manual demonstration cases

Use the administrator and service staff accounts created for your local environment. Do not use these steps against production data.

1. Sign in as service staff, create a ticket with customer, phone, device, and problem details. Submit once with a required field blank to observe browser/API validation.
2. Search for the new ticket by customer or device, then filter by status and priority. Open it and move it through a permitted status change.
3. Sign in as an administrator, create or select an active technician, assign the ticket, then reassign it. Deactivate a technician and confirm the assignment control rejects that technician.
4. Add an inventory part, restock it, and consume a quantity against the ticket. Try to consume more than available and confirm the stock remains unchanged.
5. Set a target date in the past on a non-terminal ticket and confirm the overdue indicator. Set a target within `FIXFLOW_SLA_APPROACHING_DAYS` to confirm the approaching-deadline alert. Terminal tickets should not appear overdue.
6. Open the administrator Audit Trail page and confirm the ticket, assignment, and inventory actions appear. Sign in as staff and confirm that route/API access is forbidden.
7. Submit invalid login credentials and confirm the login error is shown. Sign out and verify that the session token can no longer access protected API routes.

The deadline indicators are in-app alerts. No external email or push notification provider is configured.

## Static analysis and findings

Java Checkstyle checks tabs, final newlines, unused imports, and wildcard imports; the final run reported 16 existing wildcard-import style findings. Oxlint enables React and JSX accessibility rules; the final run reported 17 warnings and no errors, including effect-driven loading state, render-time date formatting, one memoization dependency, and remaining accessibility patterns. The actionable missing table-action headers, modal semantics, and ticket-filter expression were corrected during this work. The reported findings are style or maintainability warnings; these tools did not report a critical or high-priority defect. These scoped checks do not prove the application contains no defects.
