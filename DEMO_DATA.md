# FixFlow demonstration data

The demo dataset is fictional and is written to the configured MySQL database through the same authenticated FixFlow API used by the application. Its ticket/customer/part names start with `DEMO-PHASE2` so it is easy to recognize during a presentation.

## Add the dataset

Make sure MySQL and the FixFlow backend are running. From `backend/backend`, run:

```powershell
node .\scripts\seed-demo-data.mjs
```

The script reads the initial administrator credentials from the gitignored `application-local.properties` file, unless `FIXFLOW_ADMIN_USERNAME` and `FIXFLOW_ADMIN_PASSWORD` are set in the current environment. It does not print the credentials or token. It skips insertion if Phase 2 demo tickets already exist.

The seed adds eight tickets across received, diagnosed, in-repair, quality-check, ready, delivered, and cancelled statuses; one active demo technician assigned to those tickets; and three demo inventory parts, including low-stock parts. Ticket due dates include overdue and approaching examples. These records persist in MySQL and are synthetic training/demo data, not actual customer repairs.

## Sign in and navigate

Open `http://localhost:5173` while the frontend is running. Sign in with the application administrator credentials configured by `fixflow.admin.username` and `fixflow.admin.password` in `application-local.properties`. These are separate from the MySQL database account.

- **Dashboard:** ticket totals, status distribution, overdue/upcoming alerts, recent activity, technician workload, and low-stock warnings.
- **Repair Tickets:** search/filter, open a ticket for details, and create a ticket with **New ticket**.
- **Technicians:** view the demo technician and workload. Administrators can add, edit, or deactivate profiles.
- **Inventory:** view demo parts and low-stock warnings; authorized staff can adjust stock and record ticket part usage.
- **Analytics:** view report counts and date filters.
- **Audit trail:** inspect ticket, technician assignment, and inventory events created by the seed through the API.

To create a service-staff account for a role demonstration, use **Users** while signed in as an administrator. Sign out and test the new account in a separate session.
